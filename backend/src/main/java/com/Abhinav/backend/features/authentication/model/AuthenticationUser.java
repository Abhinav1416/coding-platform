package com.Abhinav.backend.features.authentication.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Email
    @Column(unique = true)
    private String email;

    private Boolean emailVerified = false;
    private String emailVerificationToken;
    private LocalDateTime emailVerificationTokenExpiryDate;

    @JsonIgnore
    private String password;

    private String passwordResetToken;
    private LocalDateTime passwordResetTokenExpiryDate;

    // 2FA fields
    private Boolean twoFactorEnabled = false;
    private String twoFactorToken;
    private boolean twoFactorTokenRequested = false;
    private LocalDateTime twoFactorTokenExpiryDate;

    private String refreshToken;

    public AuthenticationUser(String email, String password) {
        this.email = email;
        this.password = password;
    }
    public Long getId() {return id;}
}
