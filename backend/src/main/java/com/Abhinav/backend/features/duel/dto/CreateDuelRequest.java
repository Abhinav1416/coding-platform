package com.Abhinav.backend.features.duel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class CreateDuelRequest {

    @NotBlank(message = "CF Handle is required")
    private String handle;

    @Size(min = 1, max = 4, message = "You must provide between 1 and 4 problems")
    private List<String> problemLinks;

    @Min(value = 1, message = "Start time must be at least 1 minute")
    @Max(value = 5, message = "Start time cannot exceed 5 minutes")
    private int startsInMinutes;

    @Min(5) @Max(90)
    private int durationMinutes;
}