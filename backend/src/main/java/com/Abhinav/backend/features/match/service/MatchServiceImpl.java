package com.Abhinav.backend.features.match.service;

import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.authentication.repository.AuthenticationUserRepository;
import com.Abhinav.backend.features.exception.InvalidRequestException;
import com.Abhinav.backend.features.exception.MatchAlreadyCompletedException;
import com.Abhinav.backend.features.exception.ResourceConflictException;
import com.Abhinav.backend.features.exception.ResourceNotFoundException;
import com.Abhinav.backend.features.match.dto.*;
import com.Abhinav.backend.features.match.model.Match;
import com.Abhinav.backend.features.match.model.MatchStatus;
import com.Abhinav.backend.features.match.model.UserStats;
import com.Abhinav.backend.features.match.repository.LiveMatchStateRepository;
import com.Abhinav.backend.features.match.repository.MatchRepository;
import com.Abhinav.backend.features.match.repository.UserStatsRepository;
import com.Abhinav.backend.features.problem.dto.ProblemDetailResponse;
import com.Abhinav.backend.features.problem.model.Problem;
import com.Abhinav.backend.features.problem.repository.ProblemRepository;
import com.Abhinav.backend.features.submission.model.Submission;
import com.Abhinav.backend.features.submission.model.SubmissionStatus;
import com.Abhinav.backend.features.submission.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager; // <-- IMPORT THIS
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matchRepository;
    private final LiveMatchStateRepository liveMatchStateRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final UserStatsRepository userStatsRepository;
    private final MatchNotificationService matchNotificationService;
    private final AuthenticationUserRepository userRepository;
    private final CacheManager cacheManager; // <-- 1. INJECT THE CACHE MANAGER


    public static final long PENALTY_MINUTES = 5;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // A helper to extract username from email safely.
    private String getUsernameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "anonymous";
        }
        return email.substring(0, email.indexOf("@"));
    }

    // ... (no changes to createDuel, joinDuel, getDuelState, processDuelSubmissionResult, completeMatch) ...

    @Override
    public CreateDuelResponse createDuel(CreateDuelRequest request, Long creatorId) {
        if (request.getDifficultyMin() > request.getDifficultyMax()) {
            throw new IllegalArgumentException("Minimum difficulty cannot be greater than maximum difficulty.");
        }
        String roomCode = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        Match match = Match.builder()
                .roomCode(roomCode)
                .playerOneId(creatorId)
                .status(MatchStatus.WAITING_FOR_OPPONENT)
                .difficultyMin(request.getDifficultyMin())
                .difficultyMax(request.getDifficultyMax())
                .startDelayInMinutes(request.getStartDelayInMinutes())
                .durationInMinutes(request.getDurationInMinutes())
                .build();
        Match savedMatch = matchRepository.save(match);
        String shareableLink = frontendUrl + "/match/join?roomCode=" + roomCode; // Corrected path
        return new CreateDuelResponse(savedMatch.getId(), roomCode, shareableLink);
    }

    @Override
    @Transactional
    public JoinDuelResponse joinDuel(JoinDuelRequest request, Long joiningUserId) {
        Match match = matchRepository.findByRoomCode(request.getRoomCode().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Match room not found with code: " + request.getRoomCode()));

        if (match.getStatus() != MatchStatus.WAITING_FOR_OPPONENT) {
            throw new ResourceConflictException("This match is not waiting for an opponent.");
        }
        if (Objects.equals(match.getPlayerOneId(), joiningUserId)) {
            throw new InvalidRequestException("You cannot join a room you created.");
        }
        match.setPlayerTwoId(joiningUserId);
        match.setStatus(MatchStatus.SCHEDULED);
        Instant scheduledTime = Instant.now().plus(match.getStartDelayInMinutes(), ChronoUnit.MINUTES);
        match.setScheduledAt(scheduledTime);
        Match savedMatch = matchRepository.save(match);

        matchNotificationService.notifyPlayerJoined(savedMatch.getId(), joiningUserId);

        return new JoinDuelResponse(savedMatch.getId(), savedMatch.getScheduledAt());
    }

    // in class com.Abhinav.backend.features.match.service.MatchServiceImpl

    // in class com.Abhinav.backend.features.match.service.MatchServiceImpl

    @Override
    public DuelStateResponseDTO getDuelState(UUID matchId) {
        // --- ADD THIS CHECK AT THE TOP ---
        // First, check the definitive status from the main database
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        // If the match is already completed, throw our new specific exception.
        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new MatchAlreadyCompletedException("Match " + matchId + " is already completed.");
        }
        // --- END OF NEW CODE ---

        // The rest of the method only runs if the match is active
        LiveMatchStateDTO liveState = liveMatchStateRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Active match not found in cache for match ID: " + matchId));

        Problem problemEntity = problemRepository.findById(liveState.getProblemId())
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found for ID: " + liveState.getProblemId()));

        ProblemDetailResponse problemDTO = ProblemDetailResponse.fromEntity(problemEntity);

        List<Long> userIds = List.of(liveState.getPlayerOneId(), liveState.getPlayerTwoId());
        Map<Long, String> usernameMap = userRepository.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(
                        AuthenticationUser::getId,
                        user -> getUsernameFromEmail(user.getEmail())
                ));

        return DuelStateResponseDTO.builder()
                .liveState(liveState)
                .problemDetails(problemDTO)
                .playerOneUsername(usernameMap.get(liveState.getPlayerOneId()))
                .playerTwoUsername(usernameMap.get(liveState.getPlayerTwoId()))
                .build();
    }

    @Override
    public void processDuelSubmissionResult(UUID matchId, Long userId, SubmissionStatus submissionStatus) {
        String logPrefix = String.format("[SUBMISSION_PROCESS matchId=%s userId=%d]", matchId, userId);
        log.info("{} Received submission result with status: {}", logPrefix, submissionStatus);
        LiveMatchStateDTO liveState = liveMatchStateRepository.findById(matchId).orElse(null);

        if (liveState == null) {
            log.warn("{} Could not find live state in Redis. Match might have already completed or timed out.", logPrefix);
            return;
        }
        if (submissionStatus.equals(SubmissionStatus.ACCEPTED)) {
            log.info("{} Submission was ACCEPTED. Triggering 'sudden death' match completion.", logPrefix);
            if (userId.equals(liveState.getPlayerOneId()) && liveState.getPlayerOneFinishTime() == null) {
                liveState.setPlayerOneFinishTime(Instant.now());
            } else if (userId.equals(liveState.getPlayerTwoId()) && liveState.getPlayerTwoFinishTime() == null) {
                liveState.setPlayerTwoFinishTime(Instant.now());
            }
            liveMatchStateRepository.save(liveState, null);
            this.completeMatch(matchId);
        } else {
            log.info("{} Submission was not accepted. Updating penalties.", logPrefix);
            if (userId.equals(liveState.getPlayerOneId())) {
                liveState.setPlayerOnePenalties(liveState.getPlayerOnePenalties() + 1);
            } else if (userId.equals(liveState.getPlayerTwoId())) {
                liveState.setPlayerTwoPenalties(liveState.getPlayerTwoPenalties() + 1);
            }
            liveMatchStateRepository.save(liveState, null);

            matchNotificationService.notifyMatchUpdate(matchId, liveState);
            log.info("{} Penalties updated in Redis and notification sent.", logPrefix);
        }
    }

    @Override
    @Transactional
    public void completeMatch(UUID matchId) {
        String logPrefix = String.format("[MATCH_COMPLETION matchId=%s]", matchId);
        log.info("{} Starting match completion process...", logPrefix);
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found in database with ID: " + matchId));

        if (match.getStatus() == MatchStatus.COMPLETED) {
            log.warn("{} Match is already completed. Aborting.", logPrefix);
            return;
        }
        Optional<LiveMatchStateDTO> liveStateOpt = liveMatchStateRepository.findById(matchId);
        if (liveStateOpt.isEmpty()) {
            log.warn("{} Live match state not found in Redis. Assuming timeout completion.", logPrefix);
            match.setStatus(MatchStatus.COMPLETED);
            match.setEndedAt(Instant.now());
            matchRepository.save(match);
            updateUserStatsForDraw(match.getPlayerOneId(), match.getPlayerTwoId());
            return;
        }

        LiveMatchStateDTO liveState = liveStateOpt.get();

        Long p1Id = liveState.getPlayerOneId();
        Long p2Id = liveState.getPlayerTwoId();
        Instant startTime = liveState.getStartedAt();
        Instant p1FinishTime = liveState.getPlayerOneFinishTime();
        Instant p2FinishTime = liveState.getPlayerTwoFinishTime();
        Duration p1EffectiveTime = (p1FinishTime != null) ? Duration.between(startTime, p1FinishTime).plusMinutes(liveState.getPlayerOnePenalties() * PENALTY_MINUTES) : null;
        Duration p2EffectiveTime = (p2FinishTime != null) ? Duration.between(startTime, p2FinishTime).plusMinutes(liveState.getPlayerTwoPenalties() * PENALTY_MINUTES) : null;
        Long winnerId = null;
        boolean isDraw = false;
        if (p1EffectiveTime != null && (p2EffectiveTime == null || p1EffectiveTime.compareTo(p2EffectiveTime) < 0)) { winnerId = p1Id; } else if (p2EffectiveTime != null && (p1EffectiveTime == null || p2EffectiveTime.compareTo(p1EffectiveTime) < 0)) { winnerId = p2Id; } else if (p1EffectiveTime != null && p1EffectiveTime.equals(p2EffectiveTime)) { isDraw = true; } else { isDraw = true; }
        log.info("{} Winner determined. WinnerID: {}, isDraw: {}", logPrefix, winnerId, isDraw);
        match.setStatus(MatchStatus.COMPLETED);
        match.setEndedAt(Instant.now());
        match.setWinnerId(winnerId);
        match.setPlayerOnePenalties(liveState.getPlayerOnePenalties());
        match.setPlayerTwoPenalties(liveState.getPlayerTwoPenalties());
        match.setPlayerOneFinishTime(liveState.getPlayerOneFinishTime());
        match.setPlayerTwoFinishTime(liveState.getPlayerTwoFinishTime());
        matchRepository.save(match);
        log.info("{} Match entity updated to COMPLETED in database with final results.", logPrefix);
        updateUserStats(p1Id, p2Id, winnerId, isDraw);
        log.info("{} User stats updated for both players.", logPrefix);

        MatchResultDTO results = this.buildMatchResults(match);

        liveMatchStateRepository.deleteById(matchId);
        log.info("{} Live state for match removed from Redis.", logPrefix);

        matchNotificationService.notifyMatchEnd(matchId, results);

        log.info("{} <- Match completion process finished successfully.", logPrefix);
    }


    /**
     * Helper method to update user stats.
     * This method is now also responsible for evicting the user profiles from the cache.
     */
    private void updateUserStats(Long p1Id, Long p2Id, Long winnerId, boolean isDraw) {
        Map<Long, UserStats> statsMap = userStatsRepository.findAllById(Arrays.asList(p1Id, p2Id)).stream().collect(Collectors.toMap(UserStats::getUserId, Function.identity()));
        UserStats p1Stats = statsMap.computeIfAbsent(p1Id, id -> { UserStats newUserStats = new UserStats(); newUserStats.setUserId(id); return newUserStats; });
        UserStats p2Stats = statsMap.computeIfAbsent(p2Id, id -> { UserStats newUserStats = new UserStats(); newUserStats.setUserId(id); return newUserStats; });

        p1Stats.setDuelsPlayed(p1Stats.getDuelsPlayed() + 1);
        p2Stats.setDuelsPlayed(p2Stats.getDuelsPlayed() + 1);

        if (isDraw) {
            p1Stats.setDuelsDrawn(p1Stats.getDuelsDrawn() + 1);
            p2Stats.setDuelsDrawn(p2Stats.getDuelsDrawn() + 1);
        } else {
            if (winnerId.equals(p1Id)) {
                p1Stats.setDuelsWon(p1Stats.getDuelsWon() + 1);
                p2Stats.setDuelsLost(p2Stats.getDuelsLost() + 1);
            } else {
                p2Stats.setDuelsWon(p2Stats.getDuelsWon() + 1);
                p1Stats.setDuelsLost(p1Stats.getDuelsLost() + 1);
            }
        }
        userStatsRepository.saveAll(Arrays.asList(p1Stats, p2Stats));

        // ***** 2. EVICT CACHE AFTER UPDATING STATS *****
        // We need the usernames to evict them from the 'userProfiles' cache.
        List<AuthenticationUser> users = userRepository.findAllById(Arrays.asList(p1Id, p2Id));
        for (AuthenticationUser user : users) {
            String username = getUsernameFromEmail(user.getEmail());
            log.info("Evicting profile from cache for username: {}", username);
            // Evict the user's profile, forcing the next request to fetch from the DB
            Objects.requireNonNull(cacheManager.getCache("userProfiles")).evict(username);
        }
        // ***********************************************
    }

    private void updateUserStatsForDraw(Long p1Id, Long p2Id) {
        if (p1Id == null || p2Id == null) return;
        updateUserStats(p1Id, p2Id, null, true);
    }

    @Override
    @Transactional(readOnly = true)
    public MatchResultDTO getMatchResults(UUID matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));
        if (match.getStatus() != MatchStatus.COMPLETED) {
            throw new InvalidRequestException("Match results are not available until the match is completed.");
        }
        return buildMatchResults(match);
    }

    private MatchResultDTO buildMatchResults(Match match) {
        List<Submission> allSubmissions = submissionRepository.findByMatchIdOrderByCreatedAtAsc(match.getId());
        PlayerResultDTO playerOneResult = buildPlayerResultFromStored(match.getPlayerOneId(), match, allSubmissions);
        PlayerResultDTO playerTwoResult = buildPlayerResultFromStored(match.getPlayerTwoId(), match, allSubmissions);
        String outcome;
        if (match.getWinnerId() == null) {
            outcome = "DRAW";
        } else if (match.getWinnerId().equals(match.getPlayerOneId())) {
            outcome = "PLAYER_ONE_WIN";
        } else {
            outcome = "PLAYER_TWO_WIN";
        }
        return MatchResultDTO.builder()
                .matchId(match.getId())
                .problemId(match.getProblemId())
                .startedAt(match.getStartedAt())
                .endedAt(match.getEndedAt())
                .winnerId(match.getWinnerId())
                .outcome(outcome)
                .playerOne(playerOneResult)
                .playerTwo(playerTwoResult)
                .build();
    }

    private PlayerResultDTO buildPlayerResultFromStored(Long userId, Match match, List<Submission> allSubmissions) {
        if (userId == null) return null;
        Instant finishTime;
        int penalties;
        if (userId.equals(match.getPlayerOneId())) {
            finishTime = match.getPlayerOneFinishTime();
            penalties = match.getPlayerOnePenalties();
        } else {
            finishTime = match.getPlayerTwoFinishTime();
            penalties = match.getPlayerTwoPenalties();
        }
        Duration effectiveTime = null;
        if (finishTime != null && match.getStartedAt() != null) {
            Duration rawDuration = Duration.between(match.getStartedAt(), finishTime);
            effectiveTime = rawDuration.plus(penalties * PENALTY_MINUTES, ChronoUnit.MINUTES);
        }
        List<SubmissionTimelineDTO> timeline = allSubmissions.stream()
                .filter(s -> s.getUserId().equals(userId))
                .map(SubmissionTimelineDTO::fromEntity)
                .collect(Collectors.toList());
        return PlayerResultDTO.builder()
                .userId(userId)
                .solved(finishTime != null)
                .finishTime(finishTime)
                .penalties(penalties)
                .effectiveTime(effectiveTime)
                .submissions(timeline)
                .build();
    }

    private static final Set<MatchStatus> PAST_MATCH_STATUSES = EnumSet.of(
            MatchStatus.COMPLETED,
            MatchStatus.CANCELED,
            MatchStatus.EXPIRED
    );

    @Override
    @Transactional(readOnly = true)
    public Page<PastMatchDto> getPastMatchesForUser(Long userId, Pageable pageable) {
        Page<Match> matchesPage = matchRepository.findUserMatchesByStatus(userId, PAST_MATCH_STATUSES, pageable);
        return matchesPage.map(match -> convertToDto(match, userId));
    }


    @Override
    @Transactional(readOnly = true)
    public LobbyStateDTO getLobbyState(UUID matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        List<Long> userIds = new ArrayList<>();
        userIds.add(match.getPlayerOneId());
        if (match.getPlayerTwoId() != null) {
            userIds.add(match.getPlayerTwoId());
        }

        Map<Long, String> usernameMap = userRepository.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(
                        AuthenticationUser::getId,
                        user -> getUsernameFromEmail(user.getEmail())
                ));

        return new LobbyStateDTO(
                match.getId(),
                match.getPlayerOneId(),
                usernameMap.get(match.getPlayerOneId()),
                match.getPlayerTwoId(),
                usernameMap.get(match.getPlayerTwoId()),
                match.getStatus(),
                match.getScheduledAt(),
                match.getDurationInMinutes()
        );
    }

    private PastMatchDto convertToDto(Match match, Long currentUserId) {
        Long opponentId = Objects.equals(match.getPlayerOneId(), currentUserId)
                ? match.getPlayerTwoId()
                : match.getPlayerOneId();
        return PastMatchDto.builder()
                .matchId(match.getId())
                .status(match.getStatus())
                .result(determineResult(match, currentUserId))
                .opponentId(opponentId)
                .problemId(match.getProblemId())
                .endedAt(match.getEndedAt())
                .createdAt(match.getCreatedAt())
                .build();
    }

    private String determineResult(Match match, Long currentUserId) {
        switch (match.getStatus()) {
            case COMPLETED:
                if (match.getWinnerId() == null) { return "DRAW"; }
                return Objects.equals(match.getWinnerId(), currentUserId) ? "WIN" : "LOSS";
            case CANCELED:
                return "CANCELED";
            case EXPIRED:
                return "EXPIRED";
            default:
                return "UNKNOWN";
        }
    }
}