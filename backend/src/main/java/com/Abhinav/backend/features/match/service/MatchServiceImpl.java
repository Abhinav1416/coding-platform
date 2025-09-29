package com.Abhinav.backend.features.match.service;

import com.Abhinav.backend.features.exception.InvalidRequestException;
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
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
    public static final long PENALTY_MINUTES = 5;



    @Value("${app.frontend.url}")
    private String frontendUrl;


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

        String shareableLink = frontendUrl + "/duels/join?roomCode=" + roomCode;

        return new CreateDuelResponse(savedMatch.getId(), roomCode, shareableLink);
    }


    @Override
    @Transactional
    public JoinDuelResponse joinDuel(JoinDuelRequest request, Long joiningUserId) {
        Match match = matchRepository.findByRoomCode(request.getRoomCode().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Duel room not found with code: " + request.getRoomCode()));

        if (match.getStatus() != MatchStatus.WAITING_FOR_OPPONENT) {
            throw new ResourceConflictException("This duel is not waiting for an opponent.");
        }

        if (Objects.equals(match.getPlayerOneId(), joiningUserId)) {
            throw new InvalidRequestException("You cannot join a room you created.");
        }

        match.setPlayerTwoId(joiningUserId);
        match.setStatus(MatchStatus.SCHEDULED);

        Instant scheduledTime = Instant.now().plus(match.getStartDelayInMinutes(), ChronoUnit.MINUTES);
        match.setScheduledAt(scheduledTime);

        Match savedMatch = matchRepository.save(match);

        // TODO: Add WebSocket logic to notify Player 1 that Player 2 has joined.


        return new JoinDuelResponse(savedMatch.getId(), savedMatch.getScheduledAt());
    }


    @Override
    public DuelStateResponseDTO getDuelState(UUID matchId) {
        LiveMatchStateDTO liveState = liveMatchStateRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Active duel not found in cache for match ID: " + matchId));

        Problem problemEntity = problemRepository.findById(liveState.getProblemId())
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found for ID: " + liveState.getProblemId()));

        ProblemDetailResponse problemDTO = ProblemDetailResponse.fromEntity(problemEntity);

        return DuelStateResponseDTO.builder()
                .liveState(liveState)
                .problemDetails(problemDTO)
                .build();
    }


    @Override
    public void processDuelSubmissionResult(UUID matchId, Long userId, SubmissionStatus submissionStatus) {
        String logPrefix = String.format("[DUEL_SUBMISSION_PROCESS matchId=%s userId=%d]", matchId, userId);
        log.info("{} Received submission result with status: {}", logPrefix, submissionStatus);

        LiveMatchStateDTO liveState = liveMatchStateRepository.findById(matchId)
                .orElse(null);

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
            liveMatchStateRepository.save(liveState);

            this.completeMatch(matchId);

        } else {
            log.info("{} Submission was not accepted. Updating penalties.", logPrefix);
            if (userId.equals(liveState.getPlayerOneId())) {
                liveState.setPlayerOnePenalties(liveState.getPlayerOnePenalties() + 1);
            } else if (userId.equals(liveState.getPlayerTwoId())) {
                liveState.setPlayerTwoPenalties(liveState.getPlayerTwoPenalties() + 1);
            }
            liveMatchStateRepository.save(liveState);
            log.info("{} Penalties updated in Redis.", logPrefix);
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

        LiveMatchStateDTO liveState = liveMatchStateRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Live match state not found in Redis for ID: " + matchId));

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

        Map<Long, UserStats> statsMap = userStatsRepository.findAllById(Arrays.asList(p1Id, p2Id)).stream().collect(Collectors.toMap(UserStats::getUserId, Function.identity()));

        UserStats p1Stats = statsMap.computeIfAbsent(p1Id, id -> { UserStats newUserStats = new UserStats(); newUserStats.setUserId(id); return newUserStats; });
        UserStats p2Stats = statsMap.computeIfAbsent(p2Id, id -> { UserStats newUserStats = new UserStats(); newUserStats.setUserId(id); return newUserStats; });

        p1Stats.setDuelsPlayed(p1Stats.getDuelsPlayed() + 1);
        p2Stats.setDuelsPlayed(p2Stats.getDuelsPlayed() + 1);

        if (isDraw) { p1Stats.setDuelsDrawn(p1Stats.getDuelsDrawn() + 1); p2Stats.setDuelsDrawn(p2Stats.getDuelsDrawn() + 1); } else { if (winnerId.equals(p1Id)) { p1Stats.setDuelsWon(p1Stats.getDuelsWon() + 1); p2Stats.setDuelsLost(p2Stats.getDuelsLost() + 1); } else { p2Stats.setDuelsWon(p2Stats.getDuelsWon() + 1); p1Stats.setDuelsLost(p1Stats.getDuelsLost() + 1); } }

        userStatsRepository.saveAll(Arrays.asList(p1Stats, p2Stats));


        log.info("{} User stats updated for both players.", logPrefix);


        liveMatchStateRepository.deleteById(matchId);
        log.info("{} Live state for match removed from Redis.", logPrefix);

        // TODO: Send WebSocket notifications to both players about the final result.
        log.info("{} <- Match completion process finished successfully.", logPrefix);
    }


    @Override
    @Transactional(readOnly = true)
    public MatchResultDTO getMatchResults(UUID matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        if (match.getStatus() != MatchStatus.COMPLETED) {
            throw new InvalidRequestException("Match results are not available until the match is completed.");
        }

        List<Submission> allSubmissions = submissionRepository.findByMatchIdOrderByCreatedAtAsc(matchId);

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
        if (finishTime != null) {
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
}