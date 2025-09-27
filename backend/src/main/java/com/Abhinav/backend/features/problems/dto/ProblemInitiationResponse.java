package com.Abhinav.backend.features.problems.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ProblemInitiationResponse {
    private UUID problemId;
    private String uploadUrl;
}