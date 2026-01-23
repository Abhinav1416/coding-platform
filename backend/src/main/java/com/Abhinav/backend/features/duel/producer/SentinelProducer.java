package com.Abhinav.backend.features.duel.producer;

import com.Abhinav.backend.features.duel.dto.MatchStartEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SentinelProducer {

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    private static final String QUEUE_NAME = "match-watch-queue";

    public void sendMatchStart(MatchStartEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            log.info("================= SQS OUTBOUND =================");
            log.info("➡ [TARGET]  {}", QUEUE_NAME);
            log.info("➡ [PAYLOAD] {}", payload);
            log.info("================================================");

            sqsTemplate.send(QUEUE_NAME, payload);

        } catch (JsonProcessingException e) {
            log.error("❌ Failed to serialize MatchStartEvent", e);
            throw new RuntimeException("Serialization error", e);
        }
    }
}