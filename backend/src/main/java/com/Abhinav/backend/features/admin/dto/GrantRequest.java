package com.Abhinav.backend.features.admin.dto;


import jakarta.validation.constraints.NotBlank;


public record GrantRequest(
        @NotBlank(message = "Email cannot be blank")
        String email
) {}