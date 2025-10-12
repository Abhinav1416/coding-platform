package com.Abhinav.backend.features.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RefreshTokenRequest {
    @NotBlank
    private String refreshToken;

}
