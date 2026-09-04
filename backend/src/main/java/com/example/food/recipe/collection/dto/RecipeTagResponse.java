package com.example.food.recipe.collection.dto;

public record RecipeTagResponse(
        Long id,
        String name,
        long recipeCount
) {
}
