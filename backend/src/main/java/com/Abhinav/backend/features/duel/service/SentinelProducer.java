package com.Abhinav.backend.features.duel.service;

import com.Abhinav.backend.features.duel.dto.MatchStartEvent;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SentinelProducer {

    private final SqsTemplate sqsTemplate;
    private static final String QUEUE_NAME = "match-watch-queue";

    public void sendMatchStart(MatchStartEvent event) {
        try {
            sqsTemplate.send(QUEUE_NAME, event);
            log.info("🚀 Dispatched Match Start to Sentinel: {}", event.matchId());
        } catch (Exception e) {
            log.error("❌ Failed to send Match Start to SQS", e);
        }
    }
}