package com.Abhinav.backend.features.match.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class MatchResultDTO {
    private UUID matchId;
    private UUID problemId;
    private Instant startedAt;
    private Instant endedAt;
    private Long winnerId;
    private String outcome;
    private String winnerUsername;
    private PlayerResultDTO playerOne;
    private PlayerResultDTO playerTwo;
}