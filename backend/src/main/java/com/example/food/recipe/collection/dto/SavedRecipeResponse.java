package com.example.food.recipe.collection.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SavedRecipeResponse(
        Long id,
        String title,
        String summary,
        String searchIngredients,
        String mealType,
        String goal,
        String coverIngredient,
        LocalDateTime savedAt,
        Long collectionId,
        String collectionName,
        List<String> tags
) {
}
