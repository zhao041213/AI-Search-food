package com.example.food.admin.operation.dto;

import java.time.LocalDateTime;

public record AdminOperationLogResponse(
        Long id,
        Long adminId,
        String adminUsername,
        String operationType,
        String httpMethod,
        String requestPath,
        String operationResult,
        Integer statusCode,
        String ipAddress,
        String details,
        LocalDateTime createdAt
) {
}
