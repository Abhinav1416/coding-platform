package com.Abhinav.backend.features.match.service;

import com.Abhinav.backend.features.match.dto.LiveMatchStateDTO;
import com.Abhinav.backend.features.match.model.Match;
import com.Abhinav.backend.features.match.model.MatchStatus;
import com.Abhinav.backend.features.match.repository.MatchRepository;
import com.Abhinav.backend.features.problem.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchScheduler {

    private final MatchRepository matchRepository;
    private final ProblemRepository problemRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public static final String LIVE_MATCH_KEY_PREFIX = "live_match:";


    @Scheduled(fixedRate = 60000)
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
                // TODO: Send WebSocket notification that match was canceled
                continue;
            }

            UUID problemId = problemIdOpt.get();


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
                    .build();

            String redisKey = LIVE_MATCH_KEY_PREFIX + match.getId();
            redisTemplate.opsForValue().set(redisKey, liveState);

            log.info("Successfully started match ID: {}. Problem ID: {}. Redis key: {}", match.getId(), problemId, redisKey);

            // 4. TODO: Send WebSocket notification for MATCH_STARTED to both players
        }
    }
}