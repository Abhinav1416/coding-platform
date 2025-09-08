package com.Abhinav.backend.features.authentication.dto;

public record AuthenticationResponseBody(
        String accessToken,
        String refreshToken,
        String message
) { }
