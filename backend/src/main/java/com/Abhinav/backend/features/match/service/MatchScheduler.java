package com.Abhinav.backend.features.match.service;

import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.authentication.repository.AuthenticationUserRepository;
import com.Abhinav.backend.features.match.dto.LiveMatchStateDTO;
import com.Abhinav.backend.features.match.model.Match;
import com.Abhinav.backend.features.match.model.MatchStatus;
import com.Abhinav.backend.features.match.repository.LiveMatchStateRepository;
import com.Abhinav.backend.features.match.repository.MatchRepository;
import com.Abhinav.backend.features.problem.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchScheduler {

    private final MatchRepository matchRepository;
    private final ProblemRepository problemRepository;
    private final LiveMatchStateRepository liveMatchStateRepository;
    private final MatchNotificationService matchNotificationService;
    private final AuthenticationUserRepository userRepository;
    private final MatchService matchService; // --- 1. INJECT THE MATCH SERVICE ---

    /**
     * Extracts the username part from an email address.
     * @param email The full email address.
     * @return The string before the '@' symbol, or a fallback string.
     */
    private String getUsernameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "anonymous"; // Fallback for safety
        }
        return email.substring(0, email.indexOf("@"));
    }

    /**
     * Periodically checks for matches that are scheduled to start.
     */
    @Scheduled(fixedRate = 15000)
    @Transactional
    public void startScheduledMatches() {
        log.info("Scheduler running: Looking for matches to start...");
        List<Match> matchesToStart = matchRepository.findAllByStatusAndScheduledAtBefore(
                MatchStatus.SCHEDULED,
                Instant.now()
        );

        if (matchesToStart.isEmpty()) {
            log.info("Scheduler: No matches to start at this time.");
            return;
        }

        for (Match match : matchesToStart) {
            log.info("Scheduler: Attempting to start match ID: {}", match.getId());
            try {
                Optional<UUID> problemIdOpt = problemRepository.findRandomUnsolvedProblemForTwoUsers(
                        match.getDifficultyMin(),
                        match.getDifficultyMax(),
                        match.getPlayerOneId(),
                        match.getPlayerTwoId()
                );

                if (problemIdOpt.isEmpty()) {
                    log.warn("Could not find a suitable problem for match {}. Canceling match.", match.getId());
                    match.setStatus(MatchStatus.CANCELED);
                    matchRepository.save(match);
                    String reason = "Could not find a suitable problem for both players.";
                    matchNotificationService.notifyMatchCanceled(match.getId(), reason);
                    continue;
                }

                UUID problemId = problemIdOpt.get();
                Map<Long, String> usernameMap = userRepository.findByIdIn(List.of(match.getPlayerOneId(), match.getPlayerTwoId())).stream()
                        .collect(Collectors.toMap(AuthenticationUser::getId, user -> getUsernameFromEmail(user.getEmail())));

                match.setStatus(MatchStatus.ACTIVE);
                match.setProblemId(problemId);
                match.setStartedAt(Instant.now());
                matchRepository.save(match);

                LiveMatchStateDTO liveState = LiveMatchStateDTO.builder()
                        .matchId(match.getId())
                        .problemId(problemId)
                        .playerOneId(match.getPlayerOneId())
                        .playerTwoId(match.getPlayerTwoId())
                        .startedAt(match.getStartedAt())
                        .durationInMinutes(match.getDurationInMinutes())
                        .build();

                liveMatchStateRepository.save(liveState, (long) match.getDurationInMinutes());
                log.info(
                        "Successfully started match ID: {}. Live state created in Redis with TTL: {} minutes.",
                        match.getId(),
                        match.getDurationInMinutes()
                );

                matchNotificationService.notifyMatchStart(
                        match.getId(),
                        liveState,
                        usernameMap.get(match.getPlayerOneId()),
                        usernameMap.get(match.getPlayerTwoId())
                );
            } catch (Exception e) {
                log.error("Scheduler: Unexpected error starting match ID: {}. Canceling.", match.getId(), e);
                match.setStatus(MatchStatus.CANCELED);
                matchRepository.save(match);
                String reason = "An internal error occurred while starting the match.";
                matchNotificationService.notifyMatchCanceled(match.getId(), reason);
            }
        }
    }


    // --- 2. ADD THIS NEW METHOD ---

    /**
     * Periodically checks for active matches whose live state in Redis has expired (timed out).
     * When a timeout is detected, it triggers the full match completion logic.
     */
    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    @Transactional
    public void completeTimedOutMatches() {
        log.info("Scheduler running: Looking for timed-out matches to complete...");

        // 1. Find all matches that are still marked as ACTIVE in the main database.
        List<Match> activeMatches = matchRepository.findAllByStatus(MatchStatus.ACTIVE);

        if (activeMatches.isEmpty()) {
            log.info("Scheduler: No active matches found to check for timeout.");
            return;
        }

        for (Match match : activeMatches) {
            // 2. For each active match, check if its live state still exists in Redis.
            Optional<LiveMatchStateDTO> liveStateOpt = liveMatchStateRepository.findById(match.getId());

            // 3. If the live state is MISSING, it means the Redis key has expired (timed out).
            if (liveStateOpt.isEmpty()) {
                log.info("Scheduler: Timeout detected for match ID: {}. Triggering completion.", match.getId());
                try {
                    // 4. Call the main completeMatch logic to finalize the score, update stats,
                    // and send the MATCH_END notification.
                    matchService.completeMatch(match.getId());
                } catch (Exception e) {
                    log.error("Scheduler: Error while completing timed-out match ID: {}. It may need manual review.", match.getId(), e);
                }
            }
        }
    }
}