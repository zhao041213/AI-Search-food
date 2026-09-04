package com.example.food.admin.user.dto;

import java.util.List;

public record AdminUserPageResponse(
        List<AdminUserResponse> items,
        long total,
        int limit,
        int offset
) {
}
