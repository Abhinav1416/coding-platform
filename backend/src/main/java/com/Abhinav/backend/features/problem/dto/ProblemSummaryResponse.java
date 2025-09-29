package com.Abhinav.backend.features.problem.dto;

import com.Abhinav.backend.features.problem.model.Problem;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ProblemSummaryResponse {

    private UUID id;
    private String slug;
    private String title;
    private Integer points;
    private String status;

    public static ProblemSummaryResponse fromEntity(Problem problem) {
        return ProblemSummaryResponse.builder()
                .id(problem.getId())
                .slug(problem.getSlug())
                .title(problem.getTitle())
                .points(problem.getPoints())
                .status(problem.getStatus())
                .build();
    }
}