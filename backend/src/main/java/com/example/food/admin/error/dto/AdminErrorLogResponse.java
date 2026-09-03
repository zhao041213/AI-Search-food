package com.example.food.admin.error.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminErrorLogResponse(
        Long id,
        String sourceType,
        String severity,
        String component,
        String exceptionClass,
        String message,
        String rootCause,
        String requestMethod,
        String requestPath,
        Integer statusCode,
        Long userId,
        Long adminId,
        String ipAddress,
        String stackTrace,
        LocalDateTime createdAt
) {
}
