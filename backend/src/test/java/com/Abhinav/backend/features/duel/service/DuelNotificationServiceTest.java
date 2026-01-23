package com.Abhinav.backend.features.duel.service;

import com.Abhinav.backend.features.duel.dto.DuelStateResponse;
import com.Abhinav.backend.features.duel.model.DuelStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DuelNotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private DuelNotificationService notificationService;



    @Test
    @DisplayName("Should format topic correctly and send message")
    void testSendDuelUpdate_Success() {
        UUID duelId = UUID.randomUUID();
        DuelStateResponse response = DuelStateResponse.builder()
                .duelId(duelId)
                .status(DuelStatus.LIVE)
                .player1Handle("P1")
                .player2Handle("P2")
                .build();

        notificationService.sendDuelUpdate(duelId, response);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/duel/" + duelId),
                eq(response)
        );
    }


    @Test
    @DisplayName("🛡️ Should catch WebSocket exceptions and not crash")
    void testSendDuelUpdate_ExceptionHandling() {
        UUID duelId = UUID.randomUUID();
        DuelStateResponse response = DuelStateResponse.builder().build();

        doThrow(new MessagingException("WebSocket Down"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        assertThatCode(() -> notificationService.sendDuelUpdate(duelId, response))
                .doesNotThrowAnyException();

        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }
}