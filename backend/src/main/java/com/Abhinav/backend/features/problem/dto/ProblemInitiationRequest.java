package com.Abhinav.backend.features.problem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProblemInitiationRequest {

    @NotBlank(message = "Title cannot be blank.")
    @Size(max = 255, message = "Title cannot exceed 255 characters.")
    private String title;

    @NotBlank(message = "Slug cannot be blank.")
    @Size(max = 255, message = "Slug cannot exceed 255 characters.")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be URL-friendly (e.g., 'two-sum').")
    private String slug;

    @NotBlank(message = "Description cannot be blank.")
    private String description;

    @NotBlank(message = "Constraints cannot be blank.")
    private String constraints;

    @NotNull(message = "Points must be provided.")
    @Min(value = 1000, message = "Points cannot be negative.")
    private Integer points;

    @NotNull(message = "Time limit must be provided.")
    @Min(value = 1, message = "Time limit must be at least 1 millisecond.")
    private Integer timeLimitMs;

    @NotNull(message = "Memory limit must be provided.")
    @Min(value = 1, message = "Memory limit must be at least 1 kilobyte.")
    private Integer memoryLimitKb;

    @Valid
    @NotNull(message = "Sample test cases cannot be null.")
    @Size(min = 2, max = 2, message = "Exactly two sample test cases are required.")
    private List<SampleTestCaseDTO> sampleTestCases;

    @Size(min = 1, message = "At least one tag is required.")
    private List<String> tags = new ArrayList<>();;
}