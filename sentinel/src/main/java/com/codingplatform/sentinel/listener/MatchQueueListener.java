package com.codingplatform.sentinel.listener;

import com.codingplatform.sentinel.dto.MatchStartEvent;
import com.codingplatform.sentinel.dto.MonitoredMatch;
import com.codingplatform.sentinel.repository.MatchMonitoringService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MatchQueueListener {

    private static final Logger log = LoggerFactory.getLogger(MatchQueueListener.class);
    private final MatchMonitoringService monitoringService;

    public MatchQueueListener(MatchMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    // Listen to the match-watch-queue
    @SqsListener("match-watch-queue")
    public void receiveMatchStart(MatchStartEvent event) {
        log.info("📩 Received Match Start Order: {}", event.matchId());

        long endTime = event.startTimeEpochSeconds() + event.durationSeconds();

        MonitoredMatch match = new MonitoredMatch(
                event.matchId(),
                event.userHandles(),
                event.problemIds(),
                endTime,
                event.startTimeEpochSeconds(),
                new java.util.HashSet<>()
        );

        monitoringService.addMatch(match);

        log.info("✅ Match {} saved to Redis. Monitoring starts now.", event.matchId());
    }
}