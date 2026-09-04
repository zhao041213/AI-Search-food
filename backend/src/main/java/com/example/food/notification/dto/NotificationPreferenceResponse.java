package com.example.food.notification.dto;

public record NotificationPreferenceResponse(
        boolean pantryExpiringEnabled,
        boolean pantryExpiredEnabled,
        boolean weeklyMenuPreparationEnabled
) {
}
