package com.Abhinav.backend.features.match.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDuelRequest {

    @NotNull(message = "Minimum difficulty is required")
    @Min(value = 1000, message = "Minimum difficulty must be at least 1000")
    @Max(value = 3500, message = "Minimum difficulty cannot exceed 3500")
    private Integer difficultyMin;


    @NotNull(message = "Maximum difficulty is required")
    @Min(value = 1000, message = "Maximum difficulty must be at least 1000")
    @Max(value = 3500, message = "Maximum difficulty cannot exceed 3500")
    private Integer difficultyMax;


    @NotNull(message = "Start delay is required")
    @Min(value = 15, message = "Duel must start in at least 15 minutes")
    @Max(value = 1440, message = "Duel must start within 24 hours")
    private Integer startDelayInMinutes;
}