package com.example.food.recipe.dto;

import java.time.LocalDateTime;

public record RecipeHistorySummaryResponse(
        Long id,
        String title,
        String searchIngredients,
        String mealType,
        String goal,
        LocalDateTime savedAt
) {
}
