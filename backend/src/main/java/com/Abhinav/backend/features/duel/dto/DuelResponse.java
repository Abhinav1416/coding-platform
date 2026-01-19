package com.Abhinav.backend.features.duel.dto;

import java.util.UUID;

public record DuelResponse(UUID duelId, String roomCode, String status, String message) {}