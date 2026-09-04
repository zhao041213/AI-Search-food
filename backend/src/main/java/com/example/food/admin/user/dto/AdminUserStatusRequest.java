package com.example.food.admin.user.dto;

public record AdminUserStatusRequest(
        Boolean enabled,
        String reason
) {
}
