package com.Abhinav.backend.features.duel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitScoreRequest {
    @NotBlank(message = "Problem ID is required")
    private String problemId;

    @NotBlank(message = "Verdict is required")
    private String verdict;

    @NotNull(message = "Time taken is required")
    private Long timeTakenSeconds;
}