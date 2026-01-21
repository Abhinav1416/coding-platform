package com.Abhinav.backend.features.duel.service;

import com.Abhinav.backend.features.duel.dto.DuelStateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DuelNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendDuelUpdate(UUID duelId, DuelStateResponse duelData) {
        String destination = "/topic/duel/" + duelId;

        log.info("📡 WS Broadcast -> {}: Status={}, P1={}, P2={}",
                destination,
                duelData.getStatus(),
                duelData.getPlayer1Handle(),
                duelData.getPlayer2Handle());

        try {
            messagingTemplate.convertAndSend(destination, duelData);
        } catch (Exception e) {
            log.error("❌ Failed to send WebSocket message to {}", destination, e);
        }
    }
}