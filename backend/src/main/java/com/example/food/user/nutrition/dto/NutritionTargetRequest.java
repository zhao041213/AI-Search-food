package com.example.food.user.nutrition.dto;

import java.math.BigDecimal;

public record NutritionTargetRequest(
        Boolean enabled,
        BigDecimal caloriesKcal,
        BigDecimal proteinG,
        BigDecimal fatG,
        BigDecimal carbohydrateG
) {
}
