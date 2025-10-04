package com.Abhinav.backend.features.user.service;

import com.Abhinav.backend.features.admin.model.TemporaryPermission;
import com.Abhinav.backend.features.admin.repository.TemporaryPermissionRepository;
import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.user.dto.UserPermissionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final TemporaryPermissionRepository permissionRepository;

    public UserPermissionDto getCurrentUserPermissions(AuthenticationUser currentUser) {
        List<TemporaryPermission> activePermissions = permissionRepository
                .findByUserIdAndExpiryTimestampAfter(currentUser.getId(), LocalDateTime.now());

        List<String> permissionStrings = activePermissions.stream()
                .map(p -> {
                    if (p.getTargetProblemId() != null) {
                        return p.getPermissionType().name() + ":" + p.getTargetProblemId();
                    }
                    return p.getPermissionType().name();
                })
                .collect(Collectors.toList());

        return new UserPermissionDto(permissionStrings);
    }
}