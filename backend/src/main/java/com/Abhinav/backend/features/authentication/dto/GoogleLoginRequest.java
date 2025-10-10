 package com.Abhinav.backend.features.authentication.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "Google token is required.")
        String token
) {}