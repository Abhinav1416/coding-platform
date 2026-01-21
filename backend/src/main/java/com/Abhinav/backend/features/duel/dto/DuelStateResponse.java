package com.Abhinav.backend.features.duel.dto;

import com.Abhinav.backend.features.duel.model.DuelScoreboard;
import com.Abhinav.backend.features.duel.model.DuelStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DuelStateResponse {
    private UUID duelId;
    private DuelStatus status;
    private DuelScoreboard scoreboard;

    private String player1Handle;
    private String player2Handle;

    private Long player1UserId;
    private Long player2UserId;

    private List<String> problemLinks;
    private List<String> problemIds;

    private int durationMinutes;
    private int startsInMinutes;

    private String roomCode;

    private Long startTime;
}