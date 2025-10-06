package com.Abhinav.backend.features.match.service;

import com.Abhinav.backend.features.match.dto.LiveMatchStateDTO;
import com.Abhinav.backend.features.match.dto.MatchResultDTO;

import java.util.UUID;

public interface MatchNotificationService {
    void notifyPlayerJoined(UUID matchId, Long playerTwoId);

    void notifyMatchUpdate(UUID matchId, LiveMatchStateDTO liveState);

    void notifyMatchEnd(UUID matchId, MatchResultDTO result);

    void notifyMatchStart(UUID matchId, LiveMatchStateDTO liveState, String p1Username, String p2Username);

    void notifyMatchCanceled(UUID matchId, String reason); // <-- ADD THIS METHOD
}