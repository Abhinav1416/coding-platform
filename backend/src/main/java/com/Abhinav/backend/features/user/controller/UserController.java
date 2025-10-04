package com.Abhinav.backend.features.user.controller;

import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.user.dto.UserPermissionDto;
import com.Abhinav.backend.features.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @GetMapping("/me/permissions")
    public ResponseEntity<UserPermissionDto> getMyPermissions(@AuthenticationPrincipal AuthenticationUser user) {
        UserPermissionDto permissions = userService.getCurrentUserPermissions(user);
        return ResponseEntity.ok(permissions);
    }
}