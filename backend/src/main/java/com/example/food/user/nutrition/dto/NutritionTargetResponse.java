package com.example.food.user.nutrition.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record NutritionTargetResponse(
        boolean configured,
        boolean enabled,
        BigDecimal caloriesKcal,
        BigDecimal proteinG,
        BigDecimal fatG,
        BigDecimal carbohydrateG,
        LocalDateTime updatedAt
) {
    public static NutritionTargetResponse empty() {
        return new NutritionTargetResponse(false, false, null, null, null, null, null);
    }
}
