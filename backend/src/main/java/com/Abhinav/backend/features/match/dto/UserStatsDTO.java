package com.Abhinav.backend.features.match.dto;


import com.Abhinav.backend.features.match.model.UserStats;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserStatsDTO {
    private Long userId;
    private int duelsPlayed;
    private int duelsWon;
    private int duelsLost;
    private int duelsDrawn;

    // A handy static method to convert an Entity to a DTO
    public static UserStatsDTO fromEntity(UserStats entity) {
        return UserStatsDTO.builder()
                .userId(entity.getUserId())
                .duelsPlayed(entity.getDuelsPlayed())
                .duelsWon(entity.getDuelsWon())
                .duelsLost(entity.getDuelsLost())
                .duelsDrawn(entity.getDuelsDrawn())
                .build();
    }
}