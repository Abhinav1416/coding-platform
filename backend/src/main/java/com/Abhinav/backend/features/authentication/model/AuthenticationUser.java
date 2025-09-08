package com.Abhinav.backend.features.authentication.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity(name = "users")
public class AuthenticationUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Email
    @Column(unique = true)
    private String email;

    private Boolean emailVerified = false;
    private String emailVerificationToken = null;
    private LocalDateTime emailVerificationTokenExpiryDate = null;

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    @JsonIgnore
    private String password;

    private String passwordResetToken = null;
    private LocalDateTime passwordResetTokenExpiryDate = null;

    // 2FA fields
    private Boolean twoFactorEnabled = false;
    private String twoFactorToken = null;
    private boolean twoFactorTokenRequested = false;
    private LocalDateTime twoFactorTokenExpiryDate = null;
    private String refreshToken;

    public AuthenticationUser(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public AuthenticationUser() {}

    // --- Getters and Setters ---
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Boolean getTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(Boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }
    public String getTwoFactorToken() { return twoFactorToken; }
    public void setTwoFactorToken(String twoFactorToken) { this.twoFactorToken = twoFactorToken; }
    public boolean isTwoFactorTokenRequested() { return twoFactorTokenRequested; }
    public void setTwoFactorTokenRequested(boolean twoFactorTokenRequested) { this.twoFactorTokenRequested = twoFactorTokenRequested; }
    public LocalDateTime getTwoFactorTokenExpiryDate() { return twoFactorTokenExpiryDate; }
    public void setTwoFactorTokenExpiryDate(LocalDateTime twoFactorTokenExpiryDate) { this.twoFactorTokenExpiryDate = twoFactorTokenExpiryDate; }
    public String getPasswordResetToken() { return passwordResetToken; }
    public void setPasswordResetToken(String passwordResetToken) { this.passwordResetToken = passwordResetToken; }
    public LocalDateTime getPasswordResetTokenExpiryDate() { return passwordResetTokenExpiryDate; }
    public void setPasswordResetTokenExpiryDate(LocalDateTime passwordResetTokenExpiryDate) { this.passwordResetTokenExpiryDate = passwordResetTokenExpiryDate; }
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
    public LocalDateTime getEmailVerificationTokenExpiryDate() {return emailVerificationTokenExpiryDate;}
    public void setEmailVerificationTokenExpiryDate(LocalDateTime emailVerificationTokenExpiryDate) {this.emailVerificationTokenExpiryDate = emailVerificationTokenExpiryDate;}
    public String getEmailVerificationToken() {return emailVerificationToken;}
    public void setEmailVerificationToken(String emailVerificationToken) {this.emailVerificationToken = emailVerificationToken;}
    public String getRefreshToken() {return refreshToken;}
    public void setRefreshToken(String refreshToken) {this.refreshToken = refreshToken;}
}
