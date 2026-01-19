package com.codingplatform.sentinel.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MatchStatusProducer {

    private static final Logger log = LoggerFactory.getLogger(MatchStatusProducer.class);
    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    private static final String RESULT_QUEUE = "match-result-queue";

    public MatchStatusProducer(SqsTemplate sqsTemplate, ObjectMapper objectMapper) {
        this.sqsTemplate = sqsTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendMatchUpdate(UUID matchId, String userHandle, String problemId, String verdict, long timeConsumed, long memoryConsumed) {
        try {
            MatchUpdateEvent event = new MatchUpdateEvent(
                    matchId,
                    userHandle,
                    problemId,
                    verdict,
                    System.currentTimeMillis(),
                    timeConsumed,
                    memoryConsumed
            );

            String jsonPayload = objectMapper.writeValueAsString(event);

            sqsTemplate.send(RESULT_QUEUE, jsonPayload);

            log.info("📤 SENT EVENT: Match {} | User {} | Verdict {}", matchId, userHandle, verdict);

        } catch (Exception e) {
            log.error("❌ FAILED to send match update event", e);
        }
    }

    public record MatchUpdateEvent(
            UUID matchId,
            String userHandle,
            String problemId,
            String verdict,
            long timestamp,
            long timeConsumedMillis,
            long memoryConsumedBytes
    ) {}
}