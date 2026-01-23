package com.Abhinav.backend.features.duel.listener;

import com.Abhinav.backend.features.duel.dto.MatchUpdateEvent;
import com.Abhinav.backend.features.duel.dto.SubmitScoreRequest;
import com.Abhinav.backend.features.duel.service.DuelManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SentinelListenerTest {

    @Mock
    private DuelManager duelManager;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SentinelListener sentinelListener;



    @Test
    @DisplayName("Should successfully process valid JSON and call DuelManager")
    void testReceiveMatchUpdate_Success() throws Exception {
        String jsonPayload = "{\"matchId\":\"123\", \"userHandle\":\"tourist\", \"verdict\":\"OK\"}";
        UUID matchId = UUID.randomUUID();

        MatchUpdateEvent mockEvent = new MatchUpdateEvent(
                matchId,
                "tourist",
                "4A",
                "OK",
                123456789L,
                100L,
                2048L
        );

        when(objectMapper.readValue(jsonPayload, MatchUpdateEvent.class)).thenReturn(mockEvent);

        sentinelListener.receiveMatchUpdate(jsonPayload);

        ArgumentCaptor<SubmitScoreRequest> captor = ArgumentCaptor.forClass(SubmitScoreRequest.class);

        verify(duelManager, times(1)).submitScoreByHandle(
                eq(matchId),
                eq("tourist"),
                captor.capture()
        );

        SubmitScoreRequest request = captor.getValue();
        assertThat(request.getProblemId()).isEqualTo("4A");
        assertThat(request.getVerdict()).isEqualTo("OK");
        assertThat(request.getTimeConsumedMillis()).isEqualTo(100L);
        assertThat(request.getMemoryConsumedBytes()).isEqualTo(2048L);
    }


    @Test
    @DisplayName("Should catch Exception if JSON is malformed (No Crash)")
    void testReceiveMatchUpdate_JsonError() throws Exception {
        String badJson = "{ invalid json }";

        when(objectMapper.readValue(anyString(), eq(MatchUpdateEvent.class)))
                .thenThrow(new JsonProcessingException("Invalid format") {});

        sentinelListener.receiveMatchUpdate(badJson);

        verify(duelManager, never()).submitScoreByHandle(any(), any(), any());
    }


    @Test
    @DisplayName("Should catch RuntimeException from DuelManager (No Crash)")
    void testReceiveMatchUpdate_DuelManagerFails() throws Exception {
        String jsonPayload = "{}";

        MatchUpdateEvent mockEvent = new MatchUpdateEvent(
                UUID.randomUUID(),
                "handle",
                "A",
                "OK",
                0L,
                0L,
                0L
        );

        when(objectMapper.readValue(anyString(), eq(MatchUpdateEvent.class))).thenReturn(mockEvent);

        doThrow(new RuntimeException("Database down"))
                .when(duelManager).submitScoreByHandle(any(), any(), any());

        sentinelListener.receiveMatchUpdate(jsonPayload);

        verify(duelManager).submitScoreByHandle(any(), any(), any());
    }
}