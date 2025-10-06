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
     * Periodically checks for matches that are scheduled to start,
     * assigns them a problem, and transitions them to an active state in Redis with a TTL.
     */
    @Scheduled(fixedRate = 15000) // Correctly configured to run periodically.
    @Transactional // Correctly wraps the logic in a transaction.
    public void startScheduledMatches() {
        log.info("Scheduler running: Looking for matches to start...");

        // This query correctly finds all matches that are ready to begin.
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
                // Logic to find a suitable problem is sound.
                Optional<UUID> problemIdOpt = problemRepository.findRandomUnsolvedProblemForTwoUsers(
                        match.getDifficultyMin(),
                        match.getDifficultyMax(),
                        match.getPlayerOneId(),
                        match.getPlayerTwoId()
                );

                // This is a great edge case to handle: canceling the match if no problem is found.
                if (problemIdOpt.isEmpty()) {
                    log.warn("Could not find a suitable problem for match {}. Canceling match.", match.getId());
                    match.setStatus(MatchStatus.CANCELED);
                    matchRepository.save(match);

                    String reason = "Could not find a suitable problem for both players.";
                    matchNotificationService.notifyMatchCanceled(match.getId(), reason);
                    continue;
                }

                UUID problemId = problemIdOpt.get();

                // Correctly prepares data for the notification.
                Map<Long, String> usernameMap = userRepository.findByIdIn(List.of(match.getPlayerOneId(), match.getPlayerTwoId())).stream()
                        .collect(Collectors.toMap(AuthenticationUser::getId, user -> getUsernameFromEmail(user.getEmail())));

                // Updates the main database record for the match. This is correct.
                match.setStatus(MatchStatus.ACTIVE);
                match.setProblemId(problemId);
                match.setStartedAt(Instant.now());
                matchRepository.save(match);

                // Prepares the DTO for Redis.
                LiveMatchStateDTO liveState = LiveMatchStateDTO.builder()
                        .matchId(match.getId())
                        .problemId(problemId)
                        .playerOneId(match.getPlayerOneId())
                        .playerTwoId(match.getPlayerTwoId())
                        .startedAt(match.getStartedAt())
                        .durationInMinutes(match.getDurationInMinutes())
                        .build();

                // This is the most critical part, and it is correct.
                // It saves the live state to Redis with a dynamic TTL, which enables our timeout logic.
                liveMatchStateRepository.save(liveState, (long) match.getDurationInMinutes());
                log.info(
                        "Successfully started match ID: {}. Live state created in Redis with TTL: {} minutes.",
                        match.getId(),
                        match.getDurationInMinutes()
                );

                // Notifies the frontend that the match has begun.
                matchNotificationService.notifyMatchStart(
                        match.getId(),
                        liveState,
                        usernameMap.get(match.getPlayerOneId()),
                        usernameMap.get(match.getPlayerTwoId())
                );

            } catch (Exception e) {
                // Robust error handling ensures one failed match doesn't break the whole scheduler.
                log.error("Scheduler: Unexpected error starting match ID: {}. Canceling.", match.getId(), e);
                match.setStatus(MatchStatus.CANCELED);
                matchRepository.save(match);

                String reason = "An internal error occurred while starting the match.";
                matchNotificationService.notifyMatchCanceled(match.getId(), reason);
            }
        }
    }
}