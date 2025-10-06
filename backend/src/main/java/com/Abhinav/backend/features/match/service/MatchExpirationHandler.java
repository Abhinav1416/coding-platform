package com.Abhinav.backend.features.match.service;

import com.Abhinav.backend.features.match.model.MatchStatus;
import com.Abhinav.backend.features.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchExpirationHandler {

    private final MatchService matchService;
    private final MatchRepository matchRepository;

    @Transactional
    public void handleExpiration(UUID matchId) {
        log.info("[EXPIRATION] Received expiration event for match key: {}", matchId);

        matchRepository.findById(matchId).ifPresent(match -> {
            if (match.getStatus() == MatchStatus.ACTIVE) {
                log.info("[EXPIRATION] Match {} is still ACTIVE. Proceeding to complete it due to timeout.", matchId);
                try {
                    matchService.completeMatch(matchId);
                } catch (Exception e) {
                    log.error("[EXPIRATION] Error completing timed-out match ID: {}.", matchId, e);
                }
            } else {
                log.info("[EXPIRATION] Match {} is no longer ACTIVE (current status: {}). No action needed.", matchId, match.getStatus());
            }
        });
    }
}