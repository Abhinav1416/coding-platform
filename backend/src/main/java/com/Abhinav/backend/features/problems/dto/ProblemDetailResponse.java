package com.Abhinav.backend.features.problems.dto;

import com.Abhinav.backend.features.problems.models.Problem;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
public class ProblemDetailResponse {

    private UUID id;
    private Long authorId;
    private String status;
    private String slug;
    private String title;
    private String description;
    private String constraints;
    private Integer points;
    private Integer timeLimitMs;
    private Integer memoryLimitKb;
    private Instant createdAt;
    private List<TestCaseDetailDTO> hiddenTestCases;

    /**
     * A static factory method to convert a Problem entity into this DTO.
     * This is a clean way to handle the mapping logic.
     */
    public static ProblemDetailResponse fromEntity(Problem problem) {
        return ProblemDetailResponse.builder()
                .id(problem.getId())
                .authorId(problem.getAuthorId())
                .status(problem.getStatus())
                .slug(problem.getSlug())
                .title(problem.getTitle())
                .description(problem.getDescription())
                .constraints(problem.getConstraints())
                .points(problem.getPoints())
                .timeLimitMs(problem.getTimeLimitMs())
                .memoryLimitKb(problem.getMemoryLimitKb())
                .createdAt(problem.getCreatedAt())
                .hiddenTestCases(problem.getHiddenTestCases().stream()
                        .map(TestCaseDetailDTO::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }
}