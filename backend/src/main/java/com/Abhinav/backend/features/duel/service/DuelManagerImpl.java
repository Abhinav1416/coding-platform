package com.Abhinav.backend.features.duel.service;

import com.Abhinav.backend.features.duel.dto.*;
import com.Abhinav.backend.features.duel.model.*;
import com.Abhinav.backend.features.duel.repository.DuelRepository;
import com.Abhinav.backend.features.match.model.UserStats;
import com.Abhinav.backend.features.match.repository.UserStatsRepository;
import com.Abhinav.backend.features.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class DuelManagerImpl implements DuelManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<String> scoringScript;
    private final DuelRepository duelRepository;
    private final UserStatsRepository userStatsRepository;
    private final SentinelProducer sentinelProducer;
    private final ObjectMapper objectMapper;
    private final DuelNotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private static final String KEY_DATA = "duel:data:";
    private static final String KEY_START = "duel:start:";
    private static final String KEY_WAITING = "duel:waiting:";
    private static final String KEY_LIVE = "duel:live:";
    private static final String KEY_CODE = "duel:code:";

    private static final Pattern CF_PATTERN = Pattern.compile("codeforces\\.com/(?:contest|gym|problemset/problem)/(\\d+)(?:/problem)?/([A-Z][0-9]?)");

    @Override
    public DuelResponse createWaitingRoom(Long userId, CreateDuelRequest request) {
        UUID duelId = UUID.randomUUID();
        String handle = request.getHandle();
        String roomCode = generateRoomCode();

        log.info("Creating waiting room. User: {}, DuelID: {}, RoomCode: {}", userId, duelId, roomCode);

        List<String> parsedIds = new ArrayList<>();
        try {
            for (String link : request.getProblemLinks()) {
                parsedIds.add(extractProblemId(link));
            }
        } catch (Exception e) {
            log.error("Failed to parse problem links for duel {}: {}", duelId, e.getMessage());
            throw e;
        }

        DuelData data = new DuelData();
        data.setDuelId(duelId);
        data.setRoomCode(roomCode);
        data.setStatus(DuelStatus.WAITING);
        data.setPlayer1Handle(handle);
        data.setPlayer1UserId(userId);
        data.setScoreboard(new DuelScoreboard());
        data.setProblemLinks(request.getProblemLinks());
        data.setProblemIds(parsedIds);
        data.setStartsInMinutes(request.getStartsInMinutes());
        data.setDurationMinutes(request.getDurationMinutes());

        long waitingTtlSeconds = (long) (request.getStartsInMinutes() * 60 * 1.5);
        if (waitingTtlSeconds < 60) waitingTtlSeconds = 60;

        stringRedisTemplate.opsForValue().set(KEY_WAITING + duelId, "WAITING", Duration.ofSeconds(waitingTtlSeconds));

        redisTemplate.opsForValue().set(KEY_DATA + duelId, data, Duration.ofMinutes(5));
        stringRedisTemplate.opsForValue().set(KEY_CODE + roomCode, duelId.toString(), Duration.ofMinutes(5));

        log.info("✅ Duel {} created. Timeout Trigger: {}s", duelId, waitingTtlSeconds);
        return new DuelResponse(duelId, roomCode, DuelStatus.WAITING.name(), "Waiting for opponent...");
    }

    @Override
    public DuelResponse joinRoom(UUID duelId, Long userId, String player2Handle) {
        String dataKey = KEY_DATA + duelId;

        @SuppressWarnings("unchecked")
        List<Object> txResults = redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.watch(dataKey);

                DuelData data = (DuelData) operations.opsForValue().get(dataKey);

                if (data == null) throw new ResourceNotFoundException("Room invalid or expired.");
                if (data.getStatus() != DuelStatus.WAITING) throw new ResourceConflictException("Match already full or started.");
                if (data.getPlayer1UserId().equals(userId)) throw new InvalidRequestException("Cannot duel yourself.");

                operations.multi();

                data.setPlayer2Handle(player2Handle);
                data.setPlayer2UserId(userId);
                data.setStatus(DuelStatus.PENDING);

                long startsInSeconds = data.getStartsInMinutes() * 60L;
                long targetStartTime = Instant.now().getEpochSecond() + startsInSeconds;
                data.setStartTime(targetStartTime);

                operations.opsForValue().set(dataKey, data);
                return operations.exec();
            }
        });

        if (txResults == null || txResults.isEmpty()) {
            throw new ResourceConflictException("Join failed due to concurrent modification.");
        }

        DuelData updatedData = (DuelData) redisTemplate.opsForValue().get(dataKey);

        stringRedisTemplate.delete(KEY_WAITING + duelId);

        long activeLifeSeconds = (updatedData.getStartsInMinutes() * 60L) + (updatedData.getDurationMinutes() * 60L) + 3600;
        redisTemplate.expire(dataKey, Duration.ofSeconds(activeLifeSeconds));
        stringRedisTemplate.expire(KEY_CODE + updatedData.getRoomCode(), Duration.ofSeconds(activeLifeSeconds));

        stringRedisTemplate.opsForValue().set(KEY_START + duelId, "STARTING", Duration.ofMinutes(updatedData.getStartsInMinutes()));

        log.info("✅ Player 2 ({}) joined Duel {}. Trigger removed. Start Time: {}", player2Handle, duelId, updatedData.getStartTime());

        broadcastUpdate(duelId, updatedData);

        long delaySeconds = updatedData.getStartsInMinutes() * 60L;
        scheduler.schedule(() -> startDuel(duelId), delaySeconds, TimeUnit.SECONDS);

        return new DuelResponse(duelId, updatedData.getRoomCode(), DuelStatus.PENDING.name(), "Match starts in " + updatedData.getStartsInMinutes() + " minutes");
    }

    @Override
    public void cancelWaitingRoom(UUID duelId) {
        String dataKey = KEY_DATA + duelId;
        DuelData data = (DuelData) redisTemplate.opsForValue().get(dataKey);

        if (data != null && data.getStatus() == DuelStatus.WAITING) {
            log.info("⏰ Waiting time expired for Duel {}. Cancelling.", duelId);

            data.setStatus(DuelStatus.CANCELLED);

            DuelStateResponse response = DuelStateResponse.builder()
                    .duelId(duelId)
                    .status(DuelStatus.CANCELLED)
                    .build();

            try {
                messagingTemplate.convertAndSend("/topic/duel/" + duelId, response);
                log.info("📡 Sent CANCELLED notification for {}", duelId);
            } catch (Exception e) {
                log.error("Failed to send cancel notification", e);
            }

            redisTemplate.delete(dataKey);
            if (data.getRoomCode() != null) {
                stringRedisTemplate.delete(KEY_CODE + data.getRoomCode());
            }
        } else {
            log.warn("Could not cancel duel {}: Data null or status not WAITING.", duelId);
        }
    }


    @Override
    public void startDuel(UUID duelId) {
        String dataKey = KEY_DATA + duelId;
        DuelData data = (DuelData) redisTemplate.opsForValue().get(dataKey);

        if (data != null && data.getStatus() == DuelStatus.PENDING) {
            data.setStatus(DuelStatus.LIVE);
            data.setStartTime(Instant.now().getEpochSecond());

            redisTemplate.opsForValue().set(dataKey, data);
            stringRedisTemplate.opsForValue().set(KEY_LIVE + duelId, "LIVE", Duration.ofMinutes(data.getDurationMinutes()));
            stringRedisTemplate.delete(KEY_CODE + data.getRoomCode());

            MatchStartEvent event = new MatchStartEvent(
                    duelId,
                    List.of(data.getPlayer1Handle(), data.getPlayer2Handle()),
                    data.getProblemIds(),
                    (long) data.getDurationMinutes() * 60,
                    data.getStartTime()
            );

            log.info("Dispatching MatchStartEvent for Duel {}", duelId);
            sentinelProducer.sendMatchStart(event);
            broadcastUpdate(duelId, data);
        }
    }

    @Override
    public void submitScoreByHandle(UUID duelId, String handle, SubmitScoreRequest request) {
        log.info("⚡ Processing submission for Duel: {}, Handle: {}", duelId, handle);
        String currentTimestamp = String.valueOf(Instant.now().getEpochSecond());
        try {
            stringRedisTemplate.execute(scoringScript, Collections.singletonList(KEY_DATA + duelId), handle, request.getProblemId(), request.getVerdict(), currentTimestamp, String.valueOf(request.getTimeConsumedMillis()), String.valueOf(request.getMemoryConsumedBytes()));
            DuelData updatedData = (DuelData) redisTemplate.opsForValue().get(KEY_DATA + duelId);
            if (updatedData != null) {
                broadcastUpdate(duelId, updatedData);
                if (updatedData.getStatus() == DuelStatus.FINISHED) {
                    saveOrUpdateHistory(updatedData, false);
                }
            }
        } catch (Exception e) {
            log.error("❌ Scoring error", e);
        }
    }

    @Override
    public void endDuel(UUID duelId) {
        String dataKey = KEY_DATA + duelId;
        DuelData finalState = (DuelData) redisTemplate.opsForValue().get(dataKey);
        if (finalState != null && finalState.getStatus() != DuelStatus.FINISHED) {
            log.info("🏁 Ending Duel: {}", duelId);
            saveOrUpdateHistory(finalState, true);
            finalState.setStatus(DuelStatus.FINISHED);
            redisTemplate.opsForValue().set(dataKey, finalState, Duration.ofMinutes(10));
            redisTemplate.delete(KEY_LIVE + duelId);
            redisTemplate.delete(KEY_START + duelId);
            broadcastUpdate(duelId, finalState);
        }
    }

    @Transactional
    private void saveOrUpdateHistory(DuelData data, boolean updateStats) {
        DuelScoreboard sb = data.getScoreboard();
        String p1 = data.getPlayer1Handle();
        String p2 = data.getPlayer2Handle();
        Long p1Id = data.getPlayer1UserId();
        Long p2Id = data.getPlayer2UserId();

        var u1 = (sb.getUsers() != null) ? sb.getUsers().get(p1) : null;
        var u2 = (sb.getUsers() != null) ? sb.getUsers().get(p2) : null;

        int p1Score = (u1 != null) ? u1.getSolved() : 0;
        int p2Score = (u2 != null) ? u2.getSolved() : 0;
        long p1Penalty = (u1 != null) ? u1.getPenalty() : 0;
        long p2Penalty = (u2 != null) ? u2.getPenalty() : 0;

        String winnerHandle = null;
        Long winnerId = null;
        boolean isDraw = false;

        if (p1Score > p2Score) { winnerHandle = p1; winnerId = p1Id; }
        else if (p2Score > p1Score) { winnerHandle = p2; winnerId = p2Id; }
        else if (p1Score > 0) {
            if (p1Penalty < p2Penalty) { winnerHandle = p1; winnerId = p1Id; }
            else if (p2Penalty < p1Penalty) { winnerHandle = p2; winnerId = p2Id; }
            else { isDraw = true; }
        } else { isDraw = true; }

        String scoreboardJson = "{}";
        try { scoreboardJson = objectMapper.writeValueAsString(sb); } catch (Exception e) { log.error("Serialization error", e); }

        Optional<DuelHistory> existing = duelRepository.findByDuelId(data.getDuelId());

        if (existing.isPresent()) {
            DuelHistory history = existing.get();
            history.setPlayer1Score(p1Score);
            history.setPlayer2Score(p2Score);
            history.setWinnerHandle(winnerHandle);
            history.setWinnerId(winnerId);
            history.setScoreboardJson(scoreboardJson);
            history.setEndedAt(LocalDateTime.now());
            duelRepository.save(history);
        } else {
            DuelHistory history = DuelHistory.builder()
                    .duelId(data.getDuelId())
                    .player1Handle(p1).player2Handle(p2).player1Id(p1Id).player2Id(p2Id)
                    .player1Score(p1Score).player2Score(p2Score).winnerHandle(winnerHandle).winnerId(winnerId)
                    .startedAt(LocalDateTime.now().minusMinutes(data.getDurationMinutes())).endedAt(LocalDateTime.now())
                    .scoreboardJson(scoreboardJson).build();
            duelRepository.save(history);
            if (updateStats) updateUserStats(p1Id, p2Id, winnerId, isDraw);
        }
    }

    private void updateUserStats(Long p1Id, Long p2Id, Long winnerId, boolean isDraw) {
        try {
            UserStats stats1 = userStatsRepository.findById(p1Id).orElse(UserStats.builder().userId(p1Id).build());
            UserStats stats2 = userStatsRepository.findById(p2Id).orElse(UserStats.builder().userId(p2Id).build());
            stats1.setDuelsPlayed(stats1.getDuelsPlayed() + 1);
            stats2.setDuelsPlayed(stats2.getDuelsPlayed() + 1);
            if (isDraw) { stats1.setDuelsDrawn(stats1.getDuelsDrawn() + 1); stats2.setDuelsDrawn(stats2.getDuelsDrawn() + 1); }
            else if (winnerId != null) {
                if (winnerId.equals(p1Id)) { stats1.setDuelsWon(stats1.getDuelsWon() + 1); stats2.setDuelsLost(stats2.getDuelsLost() + 1); }
                else { stats2.setDuelsWon(stats2.getDuelsWon() + 1); stats1.setDuelsLost(stats1.getDuelsLost() + 1); }
            }
            userStatsRepository.saveAll(List.of(stats1, stats2));
            log.info("✅ User stats updated for P1({}) and P2({})", p1Id, p2Id);
        } catch (Exception e) { log.error("❌ Failed to update user stats", e); }
    }

    @Override
    public DuelStateResponse getDuelState(UUID duelId) {
        DuelData data = (DuelData) redisTemplate.opsForValue().get(KEY_DATA + duelId);
        if (data == null) return null;
        return mapToDTO(data);
    }

    public UUID getDuelIdByCode(String roomCode) {
        String uuidStr = stringRedisTemplate.opsForValue().get(KEY_CODE + roomCode);
        if (uuidStr == null) throw new ResourceNotFoundException("Invalid Room Code.");
        return UUID.fromString(uuidStr);
    }

    private DuelStateResponse mapToDTO(DuelData data) {
        return DuelStateResponse.builder().duelId(data.getDuelId()).status(data.getStatus()).scoreboard(data.getScoreboard()).player1Handle(data.getPlayer1Handle()).player2Handle(data.getPlayer2Handle()).player1UserId(data.getPlayer1UserId()).player2UserId(data.getPlayer2UserId()).problemLinks(data.getProblemLinks()).problemIds(data.getProblemIds()).durationMinutes(data.getDurationMinutes()).startsInMinutes(data.getStartsInMinutes()).startTime(data.getStartTime()).roomCode((data.getStatus() == DuelStatus.WAITING || data.getStatus() == DuelStatus.PENDING) ? data.getRoomCode() : null).build();
    }

    private void broadcastUpdate(UUID duelId, DuelData data) {
        try {
            notificationService.sendDuelUpdate(duelId, mapToDTO(data));
        } catch (Exception e) {
            log.error("WS Error", e);
        }
    }

    private String generateRoomCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private String extractProblemId(String url) {
        if (url == null || url.isBlank()) throw new InvalidRequestException("Empty URL");
        Matcher matcher = CF_PATTERN.matcher(url);
        if (matcher.find()) return matcher.group(1) + matcher.group(2);
        throw new InvalidRequestException("Invalid CF URL: " + url);
    }
}