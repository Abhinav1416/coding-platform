package com.Abhinav.backend.features.match.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinDuelResponse {
    private UUID matchId;
    private Instant scheduledAt;
}