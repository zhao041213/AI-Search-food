package com.example.food.ai.ingredient.dto;

import java.util.List;

public record IngredientRecognitionResponse(
        List<String> ingredients,
        String description,
        String provider,
        String model
) {
    public IngredientRecognitionResponse {
        ingredients = ingredients == null ? List.of() : ingredients;
        description = description == null ? "" : description;
    }
}
