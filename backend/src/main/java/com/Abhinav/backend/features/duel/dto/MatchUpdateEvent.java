package com.Abhinav.backend.features.duel.dto;

import java.util.UUID;

public record MatchUpdateEvent(
        UUID matchId,
        String userHandle,
        String problemId,
        String verdict,
        long timestamp,
        long timeConsumedMillis,
        long memoryConsumedBytes
) {}