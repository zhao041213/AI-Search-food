package com.example.food.notification.dto;

import java.util.List;

public record NotificationPageResponse(
        List<NotificationResponse> items,
        long total,
        int page,
        int size,
        int totalPages
) {
}
