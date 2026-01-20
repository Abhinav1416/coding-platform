package com.Abhinav.backend.features.duel.service;

import com.Abhinav.backend.features.duel.dto.CreateDuelRequest;
import com.Abhinav.backend.features.duel.dto.DuelResponse;
import com.Abhinav.backend.features.duel.dto.MatchStartEvent;
import com.Abhinav.backend.features.duel.dto.SubmitScoreRequest;
import com.Abhinav.backend.features.duel.model.DuelData;
import com.Abhinav.backend.features.duel.model.DuelHistory;
import com.Abhinav.backend.features.duel.model.DuelScoreboard;
import com.Abhinav.backend.features.duel.model.DuelStatus;
import com.Abhinav.backend.features.duel.repository.DuelRepository;
import com.Abhinav.backend.features.exception.InvalidRequestException;
import com.Abhinav.backend.features.exception.ResourceConflictException;
import com.Abhinav.backend.features.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
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
    private final SentinelProducer sentinelProducer;
    private final ObjectMapper objectMapper;
    private final DuelNotificationService notificationService;

    private static final String KEY_DATA = "duel:data:";
    private static final String KEY_START = "duel:start:";
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
        data.setScoreboard(new DuelScoreboard());
        data.setProblemLinks(request.getProblemLinks());
        data.setProblemIds(parsedIds);
        data.setStartsInMinutes(request.getStartsInMinutes());
        data.setDurationMinutes(request.getDurationMinutes());

        long waitingTtlSeconds = (long) (request.getStartsInMinutes() * 60 * 1.5);

        redisTemplate.opsForValue().set(KEY_DATA + duelId, data, Duration.ofSeconds(waitingTtlSeconds));
        stringRedisTemplate.opsForValue().set(KEY_CODE + roomCode, duelId.toString(), Duration.ofSeconds(waitingTtlSeconds));

        log.info("✅ Duel {} created successfully. TTL: {}s", duelId, waitingTtlSeconds);
        return new DuelResponse(duelId, roomCode, DuelStatus.WAITING.name(), "Waiting for opponent...");
    }

    @Override
    public DuelResponse joinRoom(UUID duelId, Long userId, String player2Handle) {
        log.info("Attempting join. DuelID: {}, User: {}, Handle: {}", duelId, userId, player2Handle);

        String dataKey = KEY_DATA + duelId;
        DuelData data = (DuelData) redisTemplate.opsForValue().get(dataKey);

        if (data == null) {
            log.warn("Join failed: Duel {} not found in Redis.", duelId);
            throw new ResourceNotFoundException("Room code invalid or match expired.");
        }

        if (data.getStatus() != DuelStatus.WAITING) {
            log.warn("Join failed: Duel {} status is {}", duelId, data.getStatus());
            throw new ResourceConflictException("Match is already in progress or finished.");
        }

        if (data.getPlayer1Handle().equalsIgnoreCase(player2Handle)) {
            throw new InvalidRequestException("You cannot duel yourself.");
        }

        data.setPlayer2Handle(player2Handle);
        data.setStatus(DuelStatus.PENDING);

        redisTemplate.opsForValue().set(dataKey, data);
        stringRedisTemplate.opsForValue().set(KEY_START + duelId, "STARTING", Duration.ofMinutes(data.getStartsInMinutes()));

        log.info("✅ Player 2 ({}) joined Duel {}. Countdown started.", player2Handle, duelId);

        // 📡 Notify Player 1 that Player 2 joined
        broadcastUpdate(duelId, data);

        return new DuelResponse(duelId, data.getRoomCode(), DuelStatus.PENDING.name(), "Match starts in " + data.getStartsInMinutes() + " minutes");
    }

    @Override
    public void startDuel(UUID duelId) {
        log.info("⏳ Timer expired. Starting Duel: {}", duelId);
        String dataKey = KEY_DATA + duelId;
        DuelData data = (DuelData) redisTemplate.opsForValue().get(dataKey);

        if (data != null) {
            data.setStatus(DuelStatus.LIVE);
            long startTime = Instant.now().getEpochSecond();
            data.setStartTime(startTime);

            redisTemplate.opsForValue().set(dataKey, data);
            stringRedisTemplate.opsForValue().set(KEY_LIVE + duelId, "LIVE", Duration.ofMinutes(data.getDurationMinutes()));

            MatchStartEvent event = new MatchStartEvent(
                    duelId,
                    List.of(data.getPlayer1Handle(), data.getPlayer2Handle()),
                    data.getProblemIds(),
                    (long) data.getDurationMinutes() * 60,
                    startTime
            );

            log.info("Dispatching MatchStartEvent to Sentinel for Duel {}", duelId);
            sentinelProducer.sendMatchStart(event);

            stringRedisTemplate.delete(KEY_CODE + data.getRoomCode());

            log.info("✅ Duel {} is now LIVE. StartTime: {}", duelId, startTime);

            // 📡 Notify both players: "GO!"
            broadcastUpdate(duelId, data);
        } else {
            log.error("❌ Failed to start duel {}: Data not found in Redis.", duelId);
        }
    }

    @Override
    public void submitScoreByHandle(UUID duelId, String handle, SubmitScoreRequest request) {
        log.info("⚡ Processing submission. Duel: {}, Handle: {}, Problem: {}, Verdict: {}",
                duelId, handle, request.getProblemId(), request.getVerdict());

        String currentTimestamp = String.valueOf(Instant.now().getEpochSecond());

        try {
            stringRedisTemplate.execute(
                    scoringScript,
                    Collections.singletonList(KEY_DATA + duelId),
                    handle,
                    request.getProblemId(),
                    request.getVerdict(),
                    currentTimestamp,
                    String.valueOf(request.getTimeConsumedMillis()),
                    String.valueOf(request.getMemoryConsumedBytes())
            );
            log.info("✅ Lua script executed for Duel {}", duelId);

            // Fetch updated state to broadcast to frontend
            DuelData updatedData = (DuelData) redisTemplate.opsForValue().get(KEY_DATA + duelId);
            if (updatedData != null) {
                broadcastUpdate(duelId, updatedData);
            }

        } catch (Exception e) {
            log.error("❌ Error executing scoring script for Duel {}", duelId, e);
        }
    }

    @Override
    public void endDuel(UUID duelId) {
        log.info("🏁 Ending Duel: {}", duelId);
        String dataKey = KEY_DATA + duelId;
        DuelData finalState = (DuelData) redisTemplate.opsForValue().get(dataKey);

        if (finalState != null) {
            DuelScoreboard sb = finalState.getScoreboard();
            String p1 = finalState.getPlayer1Handle();
            String p2 = finalState.getPlayer2Handle();

            // Safe null checks for user stats
            var u1 = (sb.getUsers() != null) ? sb.getUsers().get(p1) : null;
            var u2 = (sb.getUsers() != null) ? sb.getUsers().get(p2) : null;

            int p1Score = (u1 != null) ? u1.getSolved() : 0;
            int p2Score = (u2 != null) ? u2.getSolved() : 0;
            long p1Penalty = (u1 != null) ? u1.getPenalty() : 0;
            long p2Penalty = (u2 != null) ? u2.getPenalty() : 0;

            String winnerHandle = null;
            if (p1Score > p2Score) winnerHandle = p1;
            else if (p2Score > p1Score) winnerHandle = p2;
            else if (p1Score > 0) {
                if (p1Penalty < p2Penalty) winnerHandle = p1;
                else if (p2Penalty < p1Penalty) winnerHandle = p2;
            }

            log.info("Winner calculated: {} ({} vs {})", winnerHandle, p1Score, p2Score);

            String scoreboardJson = "{}";
            try {
                scoreboardJson = objectMapper.writeValueAsString(sb);
            } catch (Exception e) {
                log.error("❌ Failed to serialize scoreboard for Duel {}", duelId, e);
            }

            DuelHistory history = DuelHistory.builder()
                    .duelId(finalState.getDuelId())
                    .player1Handle(p1)
                    .player2Handle(p2)
                    .player1Score(p1Score)
                    .player2Score(p2Score)
                    .winnerHandle(winnerHandle)
                    .startedAt(LocalDateTime.now().minusMinutes(finalState.getDurationMinutes()))
                    .endedAt(LocalDateTime.now())
                    .scoreboardJson(scoreboardJson)
                    .build();

            duelRepository.save(history);
            log.info("✅ History saved to DB for Duel {}", duelId);

            finalState.setStatus(DuelStatus.FINISHED);
            // Keep in Redis briefly for late fetchers
            redisTemplate.opsForValue().set(dataKey, finalState, Duration.ofMinutes(10));

            redisTemplate.delete(KEY_LIVE + duelId);
            redisTemplate.delete(KEY_START + duelId);

            // 📡 Notify: Game Over
            broadcastUpdate(duelId, finalState);
        } else {
            log.warn("⚠️ Could not end duel {}: Data missing from Redis.", duelId);
        }
    }

    @Override
    public DuelData getDuelState(UUID duelId) {
        return (DuelData) redisTemplate.opsForValue().get(KEY_DATA + duelId);
    }

    public UUID getDuelIdByCode(String roomCode) {
        String uuidStr = stringRedisTemplate.opsForValue().get(KEY_CODE + roomCode);
        if (uuidStr == null) {
            log.warn("Room code lookup failed: {}", roomCode);
            throw new ResourceNotFoundException("Invalid Room Code.");
        }
        return UUID.fromString(uuidStr);
    }

    private void broadcastUpdate(UUID duelId, DuelData data) {
        try {
            notificationService.sendDuelUpdate(duelId, data);
        } catch (Exception e) {
            log.error("⚠️ Failed to broadcast WebSocket update for Duel {}", duelId, e);
        }
    }

    private String generateRoomCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private String extractProblemId(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidRequestException("Problem link cannot be empty.");
        }
        Matcher matcher = CF_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1) + matcher.group(2);
        }
        throw new InvalidRequestException("Invalid Codeforces URL: " + url + ". Must be a contest or problemset link.");
    }
}