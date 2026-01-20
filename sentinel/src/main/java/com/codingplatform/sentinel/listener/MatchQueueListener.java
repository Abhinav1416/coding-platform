package com.codingplatform.sentinel.listener;

import com.codingplatform.sentinel.dto.MatchStartEvent;
import com.codingplatform.sentinel.dto.MonitoredMatch;
import com.codingplatform.sentinel.repository.MatchMonitoringService;
import com.fasterxml.jackson.databind.ObjectMapper; // <--- Import this
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class MatchQueueListener {

    private static final Logger log = LoggerFactory.getLogger(MatchQueueListener.class);
    private final MatchMonitoringService monitoringService;
    private final ObjectMapper objectMapper;

    public MatchQueueListener(MatchMonitoringService monitoringService, ObjectMapper objectMapper) {
        this.monitoringService = monitoringService;
        this.objectMapper = objectMapper;
    }

    @SqsListener("match-watch-queue")
    public void receiveMatchStart(String jsonPayload) {
        try {
            MatchStartEvent event = objectMapper.readValue(jsonPayload, MatchStartEvent.class);
            log.info("📩 Received Match Start Order: {}", event.matchId());

            long endTime = event.startTimeEpochSeconds() + event.durationSeconds();

            MonitoredMatch match = new MonitoredMatch(
                    event.matchId(),
                    event.userHandles(),
                    event.problemIds(),
                    endTime,
                    event.startTimeEpochSeconds(),
                    new HashSet<>()
            );

            monitoringService.addMatch(match);

            log.info("✅ Match {} saved to Redis. Monitoring starts now.", event.matchId());
        } catch (Exception e) {
            log.error("❌ Critical Failure processing match. Payload: {}", jsonPayload, e);
            throw new RuntimeException("Forcing SQS retry due to internal error", e);
        }
    }
}