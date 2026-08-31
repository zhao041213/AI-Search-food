package com.example.food.user.health.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HealthProfileResponse(
        boolean completed,
        String ageRange,
        BigDecimal heightCm,
        BigDecimal weightKg,
        String activityLevel,
        BigDecimal bmi,
        LocalDateTime updatedAt
) {
    public static HealthProfileResponse empty() {
        return new HealthProfileResponse(false, null, null, null, null, null, null);
    }
}
