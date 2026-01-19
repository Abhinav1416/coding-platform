package com.codingplatform.sentinel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CodeforcesResponse(String status, List<CfSubmission> result) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CfSubmission(
            long id,
            long creationTimeSeconds,
            String verdict,
            CfProblem problem,
            String programmingLanguage,
            int timeConsumedMillis,
            long memoryConsumedBytes,
            int passedTestCount
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CfProblem(
            String contestId,
            String index,
            String name,
            String type,
            int rating
    ) {}
}