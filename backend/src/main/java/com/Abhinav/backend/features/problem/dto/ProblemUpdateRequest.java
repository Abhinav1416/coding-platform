package com.Abhinav.backend.features.problem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProblemUpdateRequest {

    @Size(max = 255, message = "Title cannot exceed 255 characters.")
    private String title;

    @Size(max = 255, message = "Slug cannot exceed 255 characters.")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be URL-friendly.")
    private String slug;

    private String description;
    private String constraints;

    @Min(value = 0, message = "Points cannot be negative.")
    private Integer points;

    @Min(value = 1, message = "Time limit must be at least 1 millisecond.")
    private Integer timeLimitMs;

    @Min(value = 1, message = "Memory limit must be at least 1 kilobyte.")
    private Integer memoryLimitKb;

    @Size(min = 1, message = "At least one tag is required.")
    private List<String> tags;
}