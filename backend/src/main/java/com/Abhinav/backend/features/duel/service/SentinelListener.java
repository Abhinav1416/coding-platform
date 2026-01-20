package com.Abhinav.backend.features.duel.service;

import com.Abhinav.backend.features.duel.dto.MatchUpdateEvent;
import com.Abhinav.backend.features.duel.dto.SubmitScoreRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SentinelListener {

    private final DuelManager duelManager;
    private final ObjectMapper objectMapper;

    @SqsListener("match-result-queue")
    public void receiveMatchUpdate(String rawPayload) { // <--- Receive as String
        try {
            // 1. Log the Raw Payload
            log.info("================= SQS INBOUND ==================");
            log.info("⬅ [SOURCE]  match-result-queue");
            log.info("⬅ [PAYLOAD] {}", rawPayload);
            log.info("================================================");

            // 2. Manually deserialize to your Object
            MatchUpdateEvent event = objectMapper.readValue(rawPayload, MatchUpdateEvent.class);

            // 3. Process as normal
            long relativeTimeSeconds = 0; // Placeholder or calculate using match start time

            SubmitScoreRequest request = new SubmitScoreRequest(
                    event.problemId(),
                    event.verdict(),
                    relativeTimeSeconds,
                    event.timeConsumedMillis(),
                    event.memoryConsumedBytes()
            );

            duelManager.submitScoreByHandle(event.matchId(), event.userHandle(), request);

        } catch (Exception e) {
            log.error("❌ Error processing Sentinel update. Payload: {}", rawPayload, e);
        }
    }
}