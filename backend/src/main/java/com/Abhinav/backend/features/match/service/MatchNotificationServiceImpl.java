package com.Abhinav.backend.features.match.service;

import com.Abhinav.backend.features.match.dto.LiveMatchStateDTO;
import com.Abhinav.backend.features.match.dto.MatchResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchNotificationServiceImpl implements MatchNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    // This is the WebSocket topic clients will subscribe to for a specific match.
    private String getMatchTopic(UUID matchId) {
        return "/topic/match/" + matchId;
    }

    @Override
    public void notifyPlayerJoined(UUID matchId, Long playerTwoId) {
        String destination = getMatchTopic(matchId);
        // We send a simple map as the payload. The frontend will know what this event means.
        Map<String, Object> payload = Map.of("eventType", "PLAYER_JOINED", "playerTwoId", playerTwoId);
        log.info("[WS_NOTIFY matchId={}] -> Sending PLAYER_JOINED event.", matchId);
        messagingTemplate.convertAndSend(destination, payload);
    }

    @Override
    public void notifyMatchUpdate(UUID matchId, LiveMatchStateDTO liveState) {
        String destination = getMatchTopic(matchId);
        Map<String, Object> payload = Map.of("eventType", "STATE_UPDATE", "liveState", liveState);
        log.info("[WS_NOTIFY matchId={}] -> Sending STATE_UPDATE event (e.g., penalty update).", matchId);
        messagingTemplate.convertAndSend(destination, payload);
    }

    @Override
    public void notifyMatchEnd(UUID matchId, MatchResultDTO result) {
        String destination = getMatchTopic(matchId);
        Map<String, Object> payload = Map.of("eventType", "MATCH_END", "result", result);
        log.info("[WS_NOTIFY matchId={}] -> Sending MATCH_END event.", matchId);
        messagingTemplate.convertAndSend(destination, payload);
    }


    @Override
    public void notifyMatchStart(UUID matchId, LiveMatchStateDTO liveState, String p1Username, String p2Username) {
        String destination = getMatchTopic(matchId);

        // Create a payload with liveState AND usernames as separate fields
        Map<String, Object> payload = Map.of(
                "eventType", "MATCH_START",
                "liveState", liveState, // The original, unchanged DTO
                "playerOneUsername", p1Username,
                "playerTwoUsername", p2Username
        );

        log.info("[WS_NOTIFY matchId={}] -> Sending MATCH_START event with usernames.", matchId);
        messagingTemplate.convertAndSend(destination, payload);
    }

    // --- ADD THIS IMPLEMENTATION ---
    @Override
    public void notifyMatchCanceled(UUID matchId, String reason) {
        String destination = getMatchTopic(matchId);
        Map<String, Object> payload = Map.of("eventType", "MATCH_CANCELED", "reason", reason);
        log.info("[WS_NOTIFY matchId={}] -> Sending MATCH_CANCELED event.", matchId);
        messagingTemplate.convertAndSend(destination, payload);
    }
}