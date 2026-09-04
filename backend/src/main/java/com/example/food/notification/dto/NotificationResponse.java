package com.example.food.notification.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String summary,
        String content,
        String targetPath,
        String status,
        LocalDateTime createdAt,
        LocalDateTime readAt,
        LocalDateTime archivedAt
) {
}
