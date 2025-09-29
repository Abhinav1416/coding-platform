package com.Abhinav.backend.features.match.service;

import com.Abhinav.backend.features.match.model.Match;
import com.Abhinav.backend.features.match.model.MatchStatus;
import com.Abhinav.backend.features.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchCleanupService {

    private final MatchRepository matchRepository;
    private static final int EXPIRATION_MINUTES = 15;



    @Scheduled(fixedRateString = "PT5M", initialDelayString = "PT2M")
    @Transactional
    public void expireStaleMatches() {
        log.info("[MATCH_CLEANUP] Running scheduled task to expire stale matches...");

        Instant cutoffTime = Instant.now().minus(EXPIRATION_MINUTES, ChronoUnit.MINUTES);

        List<Match> staleMatches = matchRepository.findAllByStatusAndCreatedAtBefore(
                MatchStatus.WAITING_FOR_OPPONENT,
                cutoffTime
        );

        if (staleMatches.isEmpty()) {
            log.info("[MATCH_CLEANUP] No stale matches found.");
            return;
        }

        log.info("[MATCH_CLEANUP] Found {} stale matches to expire. Expiring them now...", staleMatches.size());
        for (Match match : staleMatches) {
            match.setStatus(MatchStatus.EXPIRED);
        }

        matchRepository.saveAll(staleMatches);
        log.info("[MATCH_CLEANUP] Finished expiring stale matches.");
    }
}