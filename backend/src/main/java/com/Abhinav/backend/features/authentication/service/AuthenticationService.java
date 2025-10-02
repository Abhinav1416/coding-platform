package com.Abhinav.backend.features.authentication.service;

import com.Abhinav.backend.features.authentication.dto.*;
import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.authentication.model.Role;
import com.Abhinav.backend.features.authentication.model.RoleType;
import com.Abhinav.backend.features.authentication.repository.AuthenticationUserRepository;
import com.Abhinav.backend.features.authentication.repository.RoleRepository;
import com.Abhinav.backend.features.authentication.utils.EmailService;
import com.Abhinav.backend.features.authentication.utils.JwtService;
import com.Abhinav.backend.features.authentication.utils.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private final int DURATION_IN_MINUTES = 10;

    private final AuthenticationUserRepository authenticationUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, Object> redisTemplate;

    public AuthenticationResponseBody register(RegisterRequest request) {
        if (authenticationUserRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists.");
        }
        if (!PasswordValidator.isValid(request.password())) {
            throw new IllegalArgumentException("Password must be at least 8 characters long and include uppercase, lowercase, number, and special character.");
        }
        var user = new AuthenticationUser();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER not found in database."));
        user.setRoles(new HashSet<>(Set.of(userRole)));
        AuthenticationUser savedUser = authenticationUserRepository.save(user);
        sendEmailVerificationToken(savedUser.getEmail());
        return new AuthenticationResponseBody(null, null, "User registered successfully. Please check your email to verify your account.");
    }

    public AuthenticationResponseBody login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        AuthenticationUser user = getUser(request.email());
        if (user.getTwoFactorEnabled()) {
            String code = generateAndSaveTwoFactorCode(user);
            emailService.sendTwoFactorCode(user.getEmail(), code);
            return new AuthenticationResponseBody(null, null, "2FA code sent to your email.");
        }
        return generateTokensForUser(user);
    }

    public void logout(String token) {
        Date expirationDate = jwtService.extractExpiration(token);
        long remainingMillis = expirationDate.getTime() - System.currentTimeMillis();
        if (remainingMillis > 0) {
            String redisKey = "blocklist:" + token;
            redisTemplate.opsForValue().set(
                    redisKey,
                    "logged_out",
                    Duration.ofMillis(remainingMillis)
            );
            logger.info("Token blocklisted for logout. Key: {}", redisKey);
        }
    }

    public void sendEmailVerificationToken(String email) {
        AuthenticationUser user = authenticationUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with the specified email was not found."));
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException("This email address has already been verified.");
        }
        String emailVerificationToken = generateRandomToken();
        user.setEmailVerificationToken(passwordEncoder.encode(emailVerificationToken));
        user.setEmailVerificationTokenExpiryDate(LocalDateTime.now().plusMinutes(DURATION_IN_MINUTES));
        authenticationUserRepository.save(user);
        try {
            String subject = "Email Verification";
            String body = "Your verification code is: " + emailVerificationToken;
            emailService.sendEmail(email, subject, body);
            logger.info("Successfully sent email verification token to {}", email);
        } catch (Exception e) {
            logger.error("Failed to send email verification token to {}: {}", email, e.getMessage());
        }
    }

    public AuthenticationUser validateEmailVerificationToken(String token, String email) {
        AuthenticationUser user = getUser(email);
        if (user.getEmailVerificationToken() == null || user.getEmailVerificationTokenExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Email verification token is invalid or expired.");
        }
        if (passwordEncoder.matches(token, user.getEmailVerificationToken())) {
            user.setEmailVerified(true);
            user.setEmailVerificationToken(null);
            user.setEmailVerificationTokenExpiryDate(null);
            return authenticationUserRepository.save(user);
        } else {
            throw new IllegalArgumentException("Invalid email verification token.");
        }
    }

    public void sendPasswordResetToken(String email) {
        AuthenticationUser user = authenticationUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with the specified email was not found."));
        String passwordResetToken = generateRandomToken();
        user.setPasswordResetToken(passwordEncoder.encode(passwordResetToken));
        user.setPasswordResetTokenExpiryDate(LocalDateTime.now().plusMinutes(DURATION_IN_MINUTES));
        authenticationUserRepository.save(user);
        try {
            String subject = "Password Reset";
            String body = "Your password reset code is: " + passwordResetToken;
            emailService.sendEmail(email, subject, body);
            logger.info("Successfully sent password reset token to {}", email);
        } catch (Exception e) {
            logger.error("Failed to send password reset token to {}: {}", email, e.getMessage());
        }
    }

    public void resetPassword(String email, String newPassword, String token) {
        AuthenticationUser user = getUser(email);
        if (user.getPasswordResetToken() == null || user.getPasswordResetTokenExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Password reset token is invalid or expired.");
        }
        if (!passwordEncoder.matches(token, user.getPasswordResetToken())) {
            throw new IllegalArgumentException("Invalid password reset token.");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as the old password.");
        }
        if (!PasswordValidator.isValid(newPassword)) {
            throw new IllegalArgumentException("Password does not meet strength requirements.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiryDate(null);
        authenticationUserRepository.save(user);
    }

    public AuthenticationResponseBody verifyTwoFactor(TwoFactorRequest request) {
        AuthenticationUser user = getUser(request.getEmail());
        if (!Boolean.TRUE.equals(user.isTwoFactorTokenRequested())) {
            throw new IllegalArgumentException("You must login first before verifying 2FA.");
        }
        if (user.getTwoFactorToken() == null || user.getTwoFactorTokenExpiryDate().isBefore(LocalDateTime.now())) {
            user.setTwoFactorTokenRequested(false);
            authenticationUserRepository.save(user);
            throw new IllegalArgumentException("2FA code expired. Please login again.");
        }
        if (!user.getTwoFactorToken().equals(request.getToken())) {
            throw new IllegalArgumentException("Invalid 2FA code.");
        }
        user.setTwoFactorToken(null);
        user.setTwoFactorTokenExpiryDate(null);
        user.setTwoFactorTokenRequested(false);
        authenticationUserRepository.save(user);
        return generateTokensForUser(user);
    }

    public AuthenticationResponseBody refreshAccessToken(String refreshToken) {
        String email = jwtService.extractUsername(refreshToken);
        AuthenticationUser user = getUser(email);
        if (!jwtService.isTokenValid(refreshToken, user) || !refreshToken.equals(user.getRefreshToken())) {
            user.setRefreshToken(null);
            authenticationUserRepository.save(user);
            throw new IllegalArgumentException("Invalid or expired refresh token. Please log in again.");
        }
        String newAccessToken = jwtService.generateAccessToken(user);
        return new AuthenticationResponseBody(newAccessToken, refreshToken, "Access token refreshed successfully.");
    }

    public AuthenticationUser getUser(String email) {
        return authenticationUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    public void toggleTwoFactor(AuthenticationUser user) {
        user.setTwoFactorEnabled(!user.getTwoFactorEnabled());
        authenticationUserRepository.save(user);
    }

    public AuthenticationResponseBody generateTokensForUser(AuthenticationUser user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        user.setRefreshToken(refreshToken);
        authenticationUserRepository.save(user);
        return new AuthenticationResponseBody(accessToken, refreshToken, "Authentication succeeded.");
    }

    private String generateAndSaveTwoFactorCode(AuthenticationUser user) {
        String code = generateRandomToken();
        user.setTwoFactorToken(code);
        user.setTwoFactorTokenRequested(true);
        user.setTwoFactorTokenExpiryDate(LocalDateTime.now().plusMinutes(DURATION_IN_MINUTES));
        authenticationUserRepository.save(user);
        return code;
    }

    private String generateRandomToken() {
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(999999));
    }
}