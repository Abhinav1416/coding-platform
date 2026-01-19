package com.Abhinav.backend.features.duel.dto;

import java.util.List;
import java.util.UUID;

public record MatchStartEvent(
        UUID matchId,
        List<String> userHandles,
        List<String> problemIds,
        long durationSeconds,
        long startTimeEpochSeconds
) {}