package com.Abhinav.backend.features.user.dto;

import java.util.List;

public record UserPermissionDto(
        List<String> permissions
) {}