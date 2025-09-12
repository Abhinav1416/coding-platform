package com.Abhinav.backend.features.problemManagement.dto;

import com.Abhinav.backend.features.problemManagement.PMModel.Problem;
import com.Abhinav.backend.features.problemManagement.PMModel.Tag;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;



@Getter
@Setter
public class ProblemResponseDto {
    private UUID id;
    private String slug;
    private String title;
    private String description;
    private Integer points;
    private Set<String> tags;
    private OffsetDateTime createdAt;

    public static ProblemResponseDto fromEntity(Problem problem) {
        ProblemResponseDto dto = new ProblemResponseDto();
        dto.id = problem.getId();
        dto.slug = problem.getSlug();
        dto.title = problem.getTitle();
        dto.description = problem.getDescription();
        dto.points = problem.getPoints();
        dto.createdAt = problem.getCreatedAt();
        dto.tags = problem.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());
        return dto;
    }

}