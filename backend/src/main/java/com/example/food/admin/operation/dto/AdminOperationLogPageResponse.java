package com.example.food.admin.operation.dto;

import java.util.List;

public record AdminOperationLogPageResponse(
        List<AdminOperationLogResponse> items,
        long total,
        int limit,
        int offset
) {
}
