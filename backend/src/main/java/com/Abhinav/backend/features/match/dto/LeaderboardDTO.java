package com.Abhinav.backend.features.match.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardDTO {
    private Long userId;
    private String username;
    private int duelsWon;
    private int duelsPlayed;
    private long rank;
}