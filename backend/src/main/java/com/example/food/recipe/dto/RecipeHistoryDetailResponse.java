package com.example.food.recipe.dto;

import com.example.food.ai.recipe.dto.RecipeGenerateResponse;

import java.time.LocalDateTime;

public record RecipeHistoryDetailResponse(
        Long id,
        Long searchLogId,
        LocalDateTime savedAt,
        String searchIngredients,
        String mealType,
        String goal,
        RecipeGenerateResponse recipe
) {
}
