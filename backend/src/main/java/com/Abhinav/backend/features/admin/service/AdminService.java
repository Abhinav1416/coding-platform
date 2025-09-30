package com.Abhinav.backend.features.admin.service;

import com.Abhinav.backend.features.admin.model.PermissionType;
import com.Abhinav.backend.features.admin.model.TemporaryPermission;
import com.Abhinav.backend.features.admin.repository.TemporaryPermissionRepository;
import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.authentication.repository.AuthenticationUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final AuthenticationUserRepository userRepository;
    private final TemporaryPermissionRepository permissionRepository;

    @Value("${permissions.expiry-minutes}")
    private long permissionExpiryMinutes;

    public void grantCreatePermission(String email) {
        AuthenticationUser user = findUserByEmail(email);
        TemporaryPermission permission = new TemporaryPermission();
        permission.setUser(user);
        permission.setPermissionType(PermissionType.CREATE_PROBLEM);
        permission.setExpiryTimestamp(LocalDateTime.now().plusMinutes(permissionExpiryMinutes));
        permissionRepository.save(permission);
    }

    public void grantUpdatePermission(String email, UUID problemId) {
        AuthenticationUser user = findUserByEmail(email);
        TemporaryPermission permission = new TemporaryPermission();
        permission.setUser(user);
        permission.setPermissionType(PermissionType.UPDATE_PROBLEM);
        permission.setTargetProblemId(problemId);
        permission.setExpiryTimestamp(LocalDateTime.now().plusMinutes(permissionExpiryMinutes));
        permissionRepository.save(permission);
    }

    // CHANGE: Method now accepts String email
    public void grantDeletePermission(String email, UUID problemId) {
        AuthenticationUser user = findUserByEmail(email);
        TemporaryPermission permission = new TemporaryPermission();
        permission.setUser(user);
        permission.setPermissionType(PermissionType.DELETE_PROBLEM);
        permission.setTargetProblemId(problemId);
        permission.setExpiryTimestamp(LocalDateTime.now().plusMinutes(permissionExpiryMinutes));
        permissionRepository.save(permission);
    }

    // CHANGE: New helper method to find by email
    private AuthenticationUser findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }
}