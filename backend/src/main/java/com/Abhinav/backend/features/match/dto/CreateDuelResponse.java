package com.Abhinav.backend.features.match.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDuelResponse {
    private String roomCode;
    private String shareableLink;
}