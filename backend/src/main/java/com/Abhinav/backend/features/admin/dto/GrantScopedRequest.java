package com.Abhinav.backend.features.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;import jakarta.validation.constraints.NotBlank;

public record GrantScopedRequest(
        @NotBlank(message = "Email cannot be blank")
        String email,
        @NotNull(message = "Problem ID cannot be null")
        UUID problemId
) {}