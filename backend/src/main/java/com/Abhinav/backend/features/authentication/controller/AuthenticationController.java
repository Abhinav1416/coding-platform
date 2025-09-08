package com.Abhinav.backend.features.authentication.controller;

import com.Abhinav.backend.features.authentication.dto.AuthenticationRequestBody;
import com.Abhinav.backend.features.authentication.dto.AuthenticationResponseBody;
import com.Abhinav.backend.features.authentication.dto.Response;
import com.Abhinav.backend.features.authentication.dto.TwoFactorRequest;
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
    public AuthenticationResponseBody loginPage(@Valid @RequestBody AuthenticationRequestBody loginRequestBody) {
        return authenticationUserService.login(loginRequestBody);
    }

    @PostMapping("/register")
    public AuthenticationResponseBody registerPage(@Valid @RequestBody AuthenticationRequestBody registerRequestBody) {
        return authenticationUserService.register(registerRequestBody);
    }

    @GetMapping("/user")
    public AuthenticationUser getUser(@RequestAttribute("authenticatedUser") AuthenticationUser user) {
        return user;
    }

//    @PutMapping("/validate-email-verification-token")
//    public Response verifyEmail(@RequestParam String token, @RequestAttribute("authenticatedUser") AuthenticationUser user) {
//        authenticationUserService.validateEmailVerificationToken(token, user.getEmail());
//        return new Response("Email verified successfully.");
//    }

    @PutMapping("/validate-email-verification-token")
    public AuthenticationResponseBody verifyEmail(
            @RequestParam String email,
            @RequestParam String token) {

        AuthenticationUser user = authenticationUserService.validateEmailVerificationToken(token, email);

        // Now email is verified → generate tokens
        return authenticationUserService.generateTokensForUser(user);
    }


    @GetMapping("/send-email-verification-token")
    public Response sendEmailVerificationToken(@RequestAttribute("authenticatedUser") AuthenticationUser user) {
        authenticationUserService.sendEmailVerificationToken(user.getEmail());
        return new Response("Email verification token sent successfully.");
    }

    @PutMapping("/send-password-reset-token")
    public Response sendPasswordResetToken(@RequestParam String email) {
        authenticationUserService.sendPasswordResetToken(email);
        return new Response("Password reset token sent successfully.");
    }

    @PutMapping("/reset-password")
    public Response resetPassword(@RequestParam String newPassword, @RequestParam String token,
                                  @RequestParam String email) {
        authenticationUserService.resetPassword(email, newPassword, token);
        return new Response("Password reset successfully.");
    }

    @PostMapping("/2fa/toggle")
    public ResponseEntity<String> toggle2FA(@RequestParam String email) {
        AuthenticationUser user = authenticationUserService.getUser(email);
        authenticationUserService.toggleTwoFactor(user);
        return ResponseEntity.ok("2FA " + (user.getTwoFactorEnabled() ? "enabled" : "disabled"));
    }

    @PostMapping("/verify-2fa")
    public AuthenticationResponseBody verifyTwoFactor(@Valid @RequestBody TwoFactorRequest request) {
        return authenticationUserService.verifyTwoFactor(request);
    }

    @PostMapping("/refresh-access-token")
    public AuthenticationResponseBody refreshAccessToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        return authenticationUserService.refreshAccessToken(refreshToken);
    }

}