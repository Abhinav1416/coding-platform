package com.Abhinav.backend.features.match.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinDuelRequest {

    @NotBlank(message = "Room code is required")
    private String roomCode;
}