package com.Abhinav.backend.features.authentication.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DuelStatsDto {
    private int duelsPlayed;
    private int duelsWon;
    private int duelsLost;
    private int duelsDrawn;
    private double winRate;
}