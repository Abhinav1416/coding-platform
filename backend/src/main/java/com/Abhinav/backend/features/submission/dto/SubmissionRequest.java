package com.Abhinav.backend.features.submission.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

/**
 * DTO for accepting a new code submission from a user.
 */
@Data
public class SubmissionRequest {

    @NotNull(message = "Problem ID cannot be null.")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "Problem ID must be a valid UUID.")
    private String problemId;

    @NotBlank(message = "Language cannot be blank.")
    private String language;

    @NotBlank(message = "Code cannot be blank.")
    private String code;

    private UUID matchId;
}
