package com.Abhinav.backend.features.authentication.controller;

import com.Abhinav.backend.features.authentication.dto.*;
import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.authentication.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/authentication")
public class AuthenticationController {
    private final AuthenticationService authenticationUserService;

    public AuthenticationController(AuthenticationService authenticationUserService) {
        this.authenticationUserService = authenticationUserService;
    }

    @PostMapping("/login")
    public AuthenticationResponseBody login(@Valid @RequestBody AuthenticationRequestBody loginRequestBody) {
        return authenticationUserService.login(loginRequestBody);
    }

    @PostMapping("/register")
    public AuthenticationResponseBody register(@Valid @RequestBody AuthenticationRequestBody registerRequestBody) {
        return authenticationUserService.register(registerRequestBody);
    }

    @GetMapping("/user")
    public AuthenticationUser getUser(@RequestAttribute("authenticatedUser") AuthenticationUser user) {
        return user;
    }

    @PutMapping("/validate-email-verification-token")
    public AuthenticationResponseBody verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        AuthenticationUser user = authenticationUserService.validateEmailVerificationToken(
                request.getToken(), request.getEmail());
        return authenticationUserService.generateTokensForUser(user);
    }

    @PostMapping("/send-email-verification-token")
    public Response sendEmailVerificationToken(@RequestBody EmailTokenRequest request) {
        authenticationUserService.sendEmailVerificationToken(request.getEmail());
        return new Response("Email verification token sent successfully.");
    }

    @PostMapping("/send-password-reset-token")
    public Response sendPasswordResetToken(@Valid @RequestBody EmailTokenRequest request) {
        authenticationUserService.sendPasswordResetToken(request.getEmail());
        return new Response("Password reset token sent successfully.");
    }

    @PutMapping("/reset-password")
    public Response resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authenticationUserService.resetPassword(request.getEmail(), request.getNewPassword(), request.getToken());
        return new Response("Password reset successfully.");
    }

    @PostMapping("/2fa/toggle")
    public ResponseEntity<String> toggle2FA(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        AuthenticationUser user = authenticationUserService.getUser(email);
        authenticationUserService.toggleTwoFactor(user);
        return ResponseEntity.ok("2FA " + (user.getTwoFactorEnabled() ? "enabled" : "disabled"));
    }

    @PostMapping("/verify-2fa")
    public AuthenticationResponseBody verifyTwoFactor(@Valid @RequestBody TwoFactorRequest request) {
        return authenticationUserService.verifyTwoFactor(request);
    }

    @PostMapping("/refresh-access-token")
    public AuthenticationResponseBody refreshAccessToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authenticationUserService.refreshAccessToken(request.getRefreshToken());
    }
}