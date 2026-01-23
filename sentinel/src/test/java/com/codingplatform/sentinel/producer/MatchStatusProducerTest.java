package com.codingplatform.sentinel.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchStatusProducerTest {

    @Mock
    private SqsTemplate sqsTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MatchStatusProducer producer;

    private static final String QUEUE_NAME = "match-result-queue";



    @Test
    @DisplayName("Should create event, serialize it, and send to SQS")
    void testSendMatchUpdate_Success() throws Exception {
        UUID matchId = UUID.randomUUID();
        String handle = "tourist";
        String problem = "4A";
        String verdict = "OK";
        long time = 30L;
        long memory = 1024L;
        String expectedJson = "{\"status\":\"OK\"}";

        when(objectMapper.writeValueAsString(any(MatchStatusProducer.MatchUpdateEvent.class)))
                .thenReturn(expectedJson);

        producer.sendMatchUpdate(matchId, handle, problem, verdict, time, memory);

        verify(sqsTemplate).send(eq(QUEUE_NAME), eq(expectedJson));

        ArgumentCaptor<MatchStatusProducer.MatchUpdateEvent> captor =
                ArgumentCaptor.forClass(MatchStatusProducer.MatchUpdateEvent.class);

        verify(objectMapper).writeValueAsString(captor.capture());

        MatchStatusProducer.MatchUpdateEvent event = captor.getValue();
        assertThat(event.matchId()).isEqualTo(matchId);
        assertThat(event.userHandle()).isEqualTo(handle);
        assertThat(event.problemId()).isEqualTo(problem);
        assertThat(event.verdict()).isEqualTo(verdict);
        assertThat(event.timeConsumedMillis()).isEqualTo(time);
        assertThat(event.memoryConsumedBytes()).isEqualTo(memory);
        assertThat(event.timestamp()).isGreaterThan(0);
    }


    @Test
    @DisplayName("Should handle Serialization Error gracefully (Log & Skip)")
    void testSendMatchUpdate_SerializationFail() throws Exception {
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("Bad JSON") {});

        producer.sendMatchUpdate(UUID.randomUUID(), "user", "A", "OK", 0, 0);

        verify(sqsTemplate, never()).send(anyString(), anyString());
    }


    @Test
    @DisplayName("Should handle SQS Error gracefully (Log & No Crash)")
    void testSendMatchUpdate_SqsFail() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        doThrow(new RuntimeException("AWS Down"))
                .when(sqsTemplate).send(anyString(), anyString());

        assertThatCode(() ->
                producer.sendMatchUpdate(UUID.randomUUID(), "user", "A", "OK", 0, 0)
        ).doesNotThrowAnyException();

        verify(sqsTemplate).send(eq(QUEUE_NAME), anyString());
    }
}