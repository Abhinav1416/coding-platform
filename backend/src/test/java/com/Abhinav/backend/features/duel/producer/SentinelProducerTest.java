package com.Abhinav.backend.features.duel.producer;

import com.Abhinav.backend.features.duel.dto.MatchStartEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SentinelProducerTest {

    @Mock
    private SqsTemplate sqsTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SentinelProducer sentinelProducer;

    private static final String QUEUE_NAME = "match-watch-queue";



    @Test
    @DisplayName("Should serialize event and send to SQS")
    void testSendMatchStart_Success() throws Exception {
        MatchStartEvent event = new MatchStartEvent(
                UUID.randomUUID(),
                List.of("tourist", "petr"),
                List.of("4A"),
                1800L,
                System.currentTimeMillis()
        );
        String expectedJson = "{\"matchId\":\"some-uuid\"}";

        when(objectMapper.writeValueAsString(event)).thenReturn(expectedJson);

        sentinelProducer.sendMatchStart(event);

        verify(sqsTemplate).send(eq(QUEUE_NAME), eq(expectedJson));
    }


    @Test
    @DisplayName("Should throw RuntimeException if Serialization fails")
    void testSendMatchStart_SerializationError() throws Exception {
        MatchStartEvent event = new MatchStartEvent(
                UUID.randomUUID(), List.of(), List.of(), 0L, 0L
        );

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("Serialization failed") {});

        assertThatThrownBy(() -> sentinelProducer.sendMatchStart(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Serialization error");
    }
}