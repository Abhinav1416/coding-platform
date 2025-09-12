package com.Abhinav.backend.features.problemManagement.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class CreateProblemRequestDto {

    @NotBlank(message = "Title cannot be empty")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @NotBlank(message = "Description cannot be empty")
    private String description;

    @NotBlank(message = "Constraints cannot be empty")
    private String constraints;

    @NotNull(message = "Points cannot be null")
    @Positive(message = "Points must be a positive number")
    private Integer points;

    @NotNull(message = "Time limit cannot be null")
    @Positive(message = "Time limit must be a positive number")
    private Integer timeLimitMs;

    @NotNull(message = "Memory limit cannot be null")
    @Positive(message = "Memory limit must be a positive number")
    private Integer memoryLimitKb;

    @NotEmpty(message = "At least one tag is required")
    private Set<String> tags;

    private Set<UUID> prerequisiteIds;

    @NotEmpty(message = "Method signatures must be provided for at least one language")
    private Map<String, String> methodSignatures;
}
