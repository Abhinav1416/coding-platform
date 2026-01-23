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
    private long timeTakenSeconds;
    private long timeConsumedMillis;
    private long memoryConsumedBytes;
}