package com.Abhinav.backend.features.duel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmitScoreRequest {
    private String problemId;
    private String verdict;
    private long timeTakenSeconds;    // Relative time
    private long timeConsumedMillis;  // Execution time from Sentinel
    private long memoryConsumedBytes; // Memory from Sentinel
}