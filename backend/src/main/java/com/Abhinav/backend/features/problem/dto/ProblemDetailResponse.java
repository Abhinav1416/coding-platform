package com.Abhinav.backend.features.problem.dto;

import com.Abhinav.backend.features.problem.model.Problem;
import com.Abhinav.backend.features.problem.model.ProblemStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProblemDetailResponse {

    private UUID id;
    private ProblemStatus status;
    private String slug;
    private String title;
    private String description;
    private String constraints;
    private Integer points;
    private Integer timeLimitMs;
    private Integer memoryLimitKb;
    private Instant createdAt;
    private Object sampleTestCases;



    public static ProblemDetailResponse fromEntity(Problem problem) {
        Object parsedSampleTestCases = null;
        if (problem.getSampleTestCases() != null) {
            try {
                parsedSampleTestCases = new ObjectMapper().readValue(problem.getSampleTestCases(), List.class);
            } catch (JsonProcessingException e) {
                parsedSampleTestCases = problem.getSampleTestCases();
            }
        }

        return ProblemDetailResponse.builder()
                .id(problem.getId())
                .status(problem.getStatus())
                .slug(problem.getSlug())
                .title(problem.getTitle())
                .description(problem.getDescription())
                .constraints(problem.getConstraints())
                .points(problem.getPoints())
                .timeLimitMs(problem.getTimeLimitMs())
                .memoryLimitKb(problem.getMemoryLimitKb())
                .createdAt(problem.getCreatedAt())
                .sampleTestCases(parsedSampleTestCases)
                .build();
    }
}