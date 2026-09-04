package com.example.food.recipe.collection.dto;

public record BatchOperationFailure(
        Long recipeId,
        String reason
) {
}
