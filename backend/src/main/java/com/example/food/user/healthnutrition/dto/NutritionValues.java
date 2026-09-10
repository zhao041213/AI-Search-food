package com.example.food.user.healthnutrition.dto;

import java.math.BigDecimal;

public record NutritionValues(
        BigDecimal caloriesKcal,
        BigDecimal proteinG,
        BigDecimal fatG,
        BigDecimal carbohydrateG
) {
}
