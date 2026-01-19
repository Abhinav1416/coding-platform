package com.Abhinav.backend.features.duel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinDuelRequest {
    @NotBlank(message = "CF Handle is required")
    private String handle;
}