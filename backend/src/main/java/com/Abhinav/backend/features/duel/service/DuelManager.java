package com.Abhinav.backend.features.duel.service;

import com.Abhinav.backend.features.duel.dto.*;
import java.util.UUID;

public interface DuelManager {
    DuelResponse createWaitingRoom(Long userId, CreateDuelRequest request);

    DuelResponse joinRoom(UUID duelId, Long userId, String player2Handle);

    DuelStateResponse getDuelState(UUID duelId);

    void startDuel(UUID duelId);

    void submitScoreByHandle(UUID duelId, String handle, SubmitScoreRequest request);

    void endDuel(UUID duelId);

    UUID getDuelIdByCode(String roomCode);

    void cancelWaitingRoom(UUID duelId);
}