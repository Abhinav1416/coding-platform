package com.Abhinav.backend.features.match.service;

import com.Abhinav.backend.features.match.dto.CountdownStartPayload;
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

    private String getUsernameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "anonymous";
        }
        return email.substring(0, email.indexOf("@"));
    }

    @Scheduled(fixedRate = 15000)
    @Transactional
    public void startScheduledMatches() {
        log.info("Scheduler running: Looking for matches to start...");
        List<Match> matchesToStart = matchRepository.findAllByStatusAndScheduledAtBefore(
                MatchStatus.SCHEDULED,
                Instant.now()
        );

        if (matchesToStart.isEmpty()) {
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
                    match.setEndedAt(Instant.now());
                    matchRepository.save(match);
                    matchNotificationService.notifyMatchCanceled(match.getId(), "Could not find a suitable problem for both players.");
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

                long ttlInMinutes = match.getDurationInMinutes() + 1L;
                liveMatchStateRepository.save(liveState, ttlInMinutes);

                log.info("Successfully started match ID: {}. Live state created in Redis with TTL: {} minutes.", match.getId(), ttlInMinutes);

                matchNotificationService.notifyMatchStart(
                        match.getId(),
                        liveState,
                        usernameMap.get(match.getPlayerOneId()),
                        usernameMap.get(match.getPlayerTwoId())
                );

                long matchStartTime = match.getStartedAt().toEpochMilli();
                int matchDurationInSeconds = (int) match.getDurationInMinutes() * 60;
                CountdownStartPayload matchPayload = new CountdownStartPayload(matchStartTime, matchDurationInSeconds);
                matchNotificationService.notifyCountdownStarted(match.getId(), "MATCH_COUNTDOWN_STARTED", matchPayload);

            } catch (Exception e) {
                log.error("Scheduler: Unexpected error starting match ID: {}. Canceling.", match.getId(), e);
                match.setStatus(MatchStatus.CANCELED);
                match.setEndedAt(Instant.now());
                matchRepository.save(match);
                matchNotificationService.notifyMatchCanceled(match.getId(), "An internal error occurred while starting the match.");
            }
        }
    }
}
