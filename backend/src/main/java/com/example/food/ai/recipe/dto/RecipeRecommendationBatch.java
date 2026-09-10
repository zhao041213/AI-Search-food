package com.example.food.ai.recipe.dto;

import java.util.List;

/**
 * A group of recipe recommendations generated for one request.
 */
public record RecipeRecommendationBatch(
        String batchId,
        String mode,
        int total,
        List<RecipeGenerateResponse> recipes
) {
    public RecipeRecommendationBatch {
        recipes = recipes == null ? List.of() : List.copyOf(recipes);
    }
}
