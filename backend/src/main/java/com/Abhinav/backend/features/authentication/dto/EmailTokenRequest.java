package com.Abhinav.backend.features.authentication.dto;

import jakarta.validation.constraints.NotBlank;

public class EmailTokenRequest {
    @NotBlank
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}