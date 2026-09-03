package com.example.food.admin.error.dto;

import java.util.List;

public record AdminErrorLogPageResponse(
        List<AdminErrorLogResponse> items,
        long total,
        int limit,
        int offset
) {
}
