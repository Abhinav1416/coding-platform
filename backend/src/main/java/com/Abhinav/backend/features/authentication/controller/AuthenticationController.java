package com.Abhinav.backend.features.authentication.controller;

import com.Abhinav.backend.features.authentication.dto.*;
import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.authentication.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/authentication")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponseBody> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponseBody> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @GetMapping("/me")
    public AuthenticationUser getLoggedInUser(@AuthenticationPrincipal AuthenticationUser currentUser) {
        return currentUser;
    }

    @PutMapping("/validate-email-verification-token")
    public AuthenticationResponseBody verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        AuthenticationUser user = authenticationService.validateEmailVerificationToken(request.getToken(), request.getEmail());
        return authenticationService.generateTokensForUser(user);
    }

    @PostMapping("/send-email-verification-token")
    public Response sendEmailVerificationToken(@RequestBody EmailTokenRequest request) {
        authenticationService.sendEmailVerificationToken(request.getEmail());
        return new Response("Email verification token sent successfully.");
    }

    @PostMapping("/send-password-reset-token")
    public Response sendPasswordResetToken(@Valid @RequestBody EmailTokenRequest request) {
        authenticationService.sendPasswordResetToken(request.getEmail());
        return new Response("Password reset token sent successfully.");
    }

    @PutMapping("/change-password")
    public ResponseEntity<Response> changePassword(
            @AuthenticationPrincipal AuthenticationUser user,
            @Valid @RequestBody ChangePasswordRequest request) {
        authenticationService.changePassword(user, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(new Response("Password changed successfully."));
    }

    @PostMapping("/2fa/toggle")
    public ResponseEntity<AuthenticationResponseBody> toggle2FA(@AuthenticationPrincipal AuthenticationUser currentUser) {
        AuthenticationResponseBody responseBody = authenticationService.toggleTwoFactor(currentUser);
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/verify-2fa")
    public AuthenticationResponseBody verifyTwoFactor(@Valid @RequestBody TwoFactorRequest request) {
        return authenticationService.verifyTwoFactor(request);
    }

    @PostMapping("/refresh-access-token")
    public AuthenticationResponseBody refreshAccessToken(@RequestBody RefreshTokenRequest request) {
        return authenticationService.refreshAccessToken(request.getRefreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Response> logout(HttpServletRequest request) {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(new Response("Invalid token information."));
        }
        final String token = authHeader.substring(7);
        authenticationService.logout(token);
        return ResponseEntity.ok(new Response("Logged out successfully."));
    }
}