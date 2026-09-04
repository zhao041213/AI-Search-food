package com.example.food.admin.user.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String nickname,
        String phone,
        String avatarUrl,
        Boolean enabled,
        Boolean passwordLocked,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
}
