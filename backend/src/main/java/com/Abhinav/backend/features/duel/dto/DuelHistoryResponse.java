package com.Abhinav.backend.features.duel.dto;

import com.Abhinav.backend.features.duel.model.DuelScoreboard;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DuelHistoryResponse {
    private UUID duelId;
    private String player1Handle;
    private String player2Handle;
    private String winnerHandle;
    private int player1Score;
    private int player2Score;
    private LocalDateTime endedAt;

    // Returns object structure, NOT string
    private DuelScoreboard detailedScoreboard;
}