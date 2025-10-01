package com.Abhinav.backend.features.authentication.dto;


import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private String username;
    private DuelStatsDto duelStats;
    private long totalProblemsSolved;
    private List<SolvesByTagDto> solvesByTag;
    private List<ActivityHeatmapDto> activityHeatmap;
}