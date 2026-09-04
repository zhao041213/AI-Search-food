package com.example.food.notification.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationPreferenceRequest(
        @NotNull Boolean pantryExpiringEnabled,
        @NotNull Boolean pantryExpiredEnabled,
        @NotNull Boolean weeklyMenuPreparationEnabled
) {
}
