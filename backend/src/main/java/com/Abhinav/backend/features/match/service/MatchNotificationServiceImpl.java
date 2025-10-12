package com.Abhinav.backend.features.match.service;

import com.Abhinav.backend.features.match.dto.CountdownStartPayload;
import com.Abhinav.backend.features.match.dto.LiveMatchStateDTO;
import com.Abhinav.backend.features.match.dto.MatchResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchNotificationServiceImpl implements MatchNotificationService {

    private final SimpMessagingTemplate messagingTemplate;


    private String getMatchTopic(UUID matchId) {
        return "/topic/match/" + matchId;
    }


    @Override
    public void notifyPlayerJoined(UUID matchId, Long playerTwoId) {
        String destination = getMatchTopic(matchId);
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

        Map<String, Object> payload = Map.of(
                "eventType", "MATCH_START",
                "liveState", liveState,
                "playerOneUsername", p1Username,
                "playerTwoUsername", p2Username
        );

        log.info("[WS_NOTIFY matchId={}] -> Sending MATCH_START event with usernames.", matchId);
        messagingTemplate.convertAndSend(destination, payload);
    }


    @Override
    public void notifyMatchCanceled(UUID matchId, String reason) {
        String destination = getMatchTopic(matchId);
        Map<String, Object> payload = Map.of("eventType", "MATCH_CANCELED", "reason", reason);
        log.info("[WS_NOTIFY matchId={}] -> Sending MATCH_CANCELED event.", matchId);
        messagingTemplate.convertAndSend(destination, payload);
    }


    @Override
    public void notifyCountdownStarted(UUID matchId, String countdownType, CountdownStartPayload payload) {
        String destination = "/topic/match/" + matchId + "/countdown";

        Map<String, Object> message = new HashMap<>();
        message.put("eventType", countdownType);
        message.put("payload", payload);

        log.info("Sending {} notification to {}. StartTime: {}, Duration: {}s",
                countdownType, destination, payload.getStartTime(), payload.getDuration());

        messagingTemplate.convertAndSend(destination, message);
    }
}