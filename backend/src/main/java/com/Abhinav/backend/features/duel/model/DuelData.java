package com.Abhinav.backend.features.duel.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DuelData {
    private UUID duelId;
    private DuelStatus status;
    private DuelScoreboard scoreboard;
    private String player1Handle;
    private String player2Handle;
    private List<String> problemLinks;
    private int durationMinutes;
    private int startsInMinutes;
    private String roomCode;
    private List<String> problemIds;

    // Ensure this field exists for relative time calculation
    private Long startTime;
}