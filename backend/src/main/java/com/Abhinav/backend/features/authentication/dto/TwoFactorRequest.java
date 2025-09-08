package com.Abhinav.backend.features.authentication.dto;

public class TwoFactorRequest {
    private String email;
    private String code;

    public TwoFactorRequest() {}

    public TwoFactorRequest(String email, String code) {
        this.email = email;
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public String getCode() {
        return code;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setCode(String code) {
        this.code = code;
    }
}