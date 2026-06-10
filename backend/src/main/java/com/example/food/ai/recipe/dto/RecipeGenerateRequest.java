package com.example.food.ai.recipe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecipeGenerateRequest(
        @NotBlank
        @Size(max = 240)
        String ingredients,
        String mealType,
        String goal,
        String searchMode
) {
}
