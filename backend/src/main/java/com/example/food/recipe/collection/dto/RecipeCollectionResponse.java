package com.example.food.recipe.collection.dto;

import java.time.LocalDateTime;

public record RecipeCollectionResponse(
        Long id,
        String name,
        boolean defaultCollection,
        long recipeCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
