package com.Abhinav.backend.features.duel.service;

import com.Abhinav.backend.features.duel.dto.CreateDuelRequest;
import com.Abhinav.backend.features.duel.dto.DuelResponse;
import com.Abhinav.backend.features.duel.dto.SubmitScoreRequest;
import com.Abhinav.backend.features.duel.model.DuelData;
import java.util.UUID;

public interface DuelManager {
    DuelResponse createWaitingRoom(Long userId, CreateDuelRequest request);

    DuelResponse joinRoom(UUID duelId, Long userId, String handle);

    void submitScoreByHandle(UUID duelId, String handle, SubmitScoreRequest request);

    DuelData getDuelState(UUID duelId);

    void startDuel(UUID duelId);

    void endDuel(UUID duelId);

    UUID getDuelIdByCode(String roomCode);
}