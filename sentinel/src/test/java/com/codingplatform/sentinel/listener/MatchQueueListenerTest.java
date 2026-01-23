package com.codingplatform.sentinel.listener;

import com.codingplatform.sentinel.dto.MatchStartEvent;
import com.codingplatform.sentinel.dto.MonitoredMatch;
import com.codingplatform.sentinel.repository.MatchMonitoringService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchQueueListenerTest {

    @Mock
    private MatchMonitoringService monitoringService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MatchQueueListener matchQueueListener;



    @Test
    @DisplayName("Should deserialize JSON and add match to monitoring service")
    void testReceiveMatchStart_Success() throws Exception {
        String jsonPayload = "{\"matchId\":\"...\"}";
        UUID matchId = UUID.randomUUID();
        long startTime = 1000L;
        long duration = 300L;

        MatchStartEvent mockEvent = new MatchStartEvent(
                matchId,
                List.of("tourist"),
                List.of("4A"),
                duration,
                startTime
        );

        when(objectMapper.readValue(jsonPayload, MatchStartEvent.class)).thenReturn(mockEvent);

        matchQueueListener.receiveMatchStart(jsonPayload);

        ArgumentCaptor<MonitoredMatch> captor = ArgumentCaptor.forClass(MonitoredMatch.class);
        verify(monitoringService).addMatch(captor.capture());

        MonitoredMatch savedMatch = captor.getValue();

        assertThat(savedMatch.matchId()).isEqualTo(matchId);
        assertThat(savedMatch.userHandles()).contains("tourist");
        assertThat(savedMatch.endTimeEpochSeconds()).isEqualTo(startTime + duration);
    }


    @Test
    @DisplayName("Should throw RuntimeException on JSON error (Force Retry)")
    void testReceiveMatchStart_JsonError() throws Exception {
        String badJson = "{ invalid }";

        when(objectMapper.readValue(eq(badJson), eq(MatchStartEvent.class)))
                .thenThrow(new JsonProcessingException("Bad format") {});

        assertThatThrownBy(() -> matchQueueListener.receiveMatchStart(badJson))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Forcing SQS retry");

        verify(monitoringService, never()).addMatch(any());
    }

    @Test
    @DisplayName("🚨 Should throw RuntimeException if Service fails (Force Retry)")
    void testReceiveMatchStart_ServiceFailure() throws Exception {
        MatchStartEvent mockEvent = new MatchStartEvent(
                UUID.randomUUID(), List.of(), List.of(), 100L, 100L
        );
        when(objectMapper.readValue(anyString(), eq(MatchStartEvent.class))).thenReturn(mockEvent);

        doThrow(new RuntimeException("Redis Down"))
                .when(monitoringService).addMatch(any());

        assertThatThrownBy(() -> matchQueueListener.receiveMatchStart("{}"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Forcing SQS retry");
    }
}