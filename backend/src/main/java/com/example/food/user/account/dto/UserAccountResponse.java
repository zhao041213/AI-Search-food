package com.example.food.user.account.dto;

import java.time.LocalDateTime;

public record UserAccountResponse(
        Long id,
        String nickname,
        String phone,
        String avatarUrl,
        LocalDateTime registeredAt,
        LocalDateTime lastLoginAt,
        String status
) {
}
