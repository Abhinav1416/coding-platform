package com.codingplatform.sentinel.service;

import com.codingplatform.sentinel.client.CodeforcesApiClient;
import com.codingplatform.sentinel.dto.CodeforcesResponse;
import com.codingplatform.sentinel.dto.MonitoredMatch;
import com.codingplatform.sentinel.producer.MatchStatusProducer;
import com.codingplatform.sentinel.repository.MatchMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SentinelPollingService {

    private static final Logger log = LoggerFactory.getLogger(SentinelPollingService.class);

    private final MatchMonitoringService monitoringService;
    private final CodeforcesApiClient apiClient;
    private final MatchStatusProducer producer;

    private long nextPollTime = 0;
    private long currentBackoff = 15000;
    private long lastFailureBackoff = 0;

    private static final long MIN_INTERVAL = 15000;
    private static final long MAX_INTERVAL = 300000;

    public SentinelPollingService(MatchMonitoringService monitoringService,
                                  CodeforcesApiClient apiClient,
                                  MatchStatusProducer producer) {
        this.monitoringService = monitoringService;
        this.apiClient = apiClient;
        this.producer = producer;
    }

    @Scheduled(fixedRate = 5000)
    public void pollMatches() {
        long now = System.currentTimeMillis();

        if (now < nextPollTime) return;

        List<MonitoredMatch> activeMatches = monitoringService.getAllActiveMatches();
        if (activeMatches.isEmpty()) {
            nextPollTime = now + 5000;
            return;
        }

        log.info("🔄 Polling {} active matches...", activeMatches.size());

        try {
            processMatches(activeMatches);

            if (currentBackoff > MIN_INTERVAL) {

                long dangerLine = Math.max(lastFailureBackoff, MIN_INTERVAL);
                long newSpeed = (currentBackoff + dangerLine) / 2;

                if (newSpeed >= currentBackoff - 1000) {
                    newSpeed = currentBackoff - 5000;
                }

                currentBackoff = Math.max(newSpeed, MIN_INTERVAL);

                log.info("✅ API Success. ADAPTIVE RECOVERY: Target={}s", currentBackoff/1000);

                if (lastFailureBackoff > 0) {
                    lastFailureBackoff = Math.max(0, lastFailureBackoff - 5000);
                }
            } else {
                lastFailureBackoff = 0;
            }

            nextPollTime = now + currentBackoff;

        } catch (Exception e) {
            lastFailureBackoff = currentBackoff;
            currentBackoff = Math.min(currentBackoff * 2, MAX_INTERVAL);

            log.error("⚠️ API FAILURE! Doubling backoff to {}s.", currentBackoff/1000);

            nextPollTime = now + currentBackoff;
        }
    }

    private void processMatches(List<MonitoredMatch> activeMatches) {
        long nowEpoch = Instant.now().getEpochSecond();

        for (MonitoredMatch match : activeMatches) {
            if (nowEpoch > match.endTimeEpochSeconds()) {
                log.info("🏁 Match {} has ended. Removing from monitoring.", match.matchId());
                monitoringService.removeMatch(match.matchId());
                continue;
            }

            for (String handle : match.userHandles()) {
                checkUserSubmissions(handle, match);
            }
        }
    }

    private void checkUserSubmissions(String handle, MonitoredMatch match) {
        List<CodeforcesResponse.CfSubmission> submissions;

        try {
            submissions = apiClient.getRecentSubmissions(handle);
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "";
            if (errorMsg.contains("404") || errorMsg.contains("400") || errorMsg.contains("FAILED")) {
                log.warn("⚠️ User handle '{}' invalid/private. Skipping.", handle);
                return;
            }
            throw e;
        }

        boolean stateChanged = false;
        MonitoredMatch currentMatchState = match;

        for (CodeforcesResponse.CfSubmission sub : submissions) {
            if (sub.creationTimeSeconds() < match.startTimeEpochSeconds()) continue;
            if (match.processedSubmissionIds().contains(sub.id())) continue;
            if (!isProblemInMatch(sub.problem(), match)) continue;
            if (sub.verdict() == null || "TESTING".equals(sub.verdict())) continue;

            String fullProblemId = sub.problem().contestId() + sub.problem().index();

            String logMsg = String.format(
                    "User: %s | Prob: %s | Verdict: %s | Time: %dms",
                    handle, sub.problem().index(), sub.verdict(), sub.timeConsumedMillis()
            );

            if ("OK".equals(sub.verdict())) {
                log.info("✅ SUCCESS (AC)! {}", logMsg);

                producer.sendMatchUpdate(
                        match.matchId(),
                        handle,
                        fullProblemId,
                        "OK",
                        sub.timeConsumedMillis(),
                        sub.memoryConsumedBytes()
                );

            } else {
                log.info("❌ FAILED ATTEMPT! {}", logMsg);

                producer.sendMatchUpdate(
                        match.matchId(),
                        handle,
                        fullProblemId,
                        sub.verdict(),
                        sub.timeConsumedMillis(),
                        sub.memoryConsumedBytes()
                );
            }

            currentMatchState = currentMatchState.withProcessedId(sub.id());
            stateChanged = true;
        }

        if (stateChanged) {
            monitoringService.addMatch(currentMatchState);
        }
    }

    private boolean isProblemInMatch(CodeforcesResponse.CfProblem problem, MonitoredMatch match) {
        String fullProblemId = problem.contestId() + problem.index();
        return match.problemIds().contains(fullProblemId);
    }
}