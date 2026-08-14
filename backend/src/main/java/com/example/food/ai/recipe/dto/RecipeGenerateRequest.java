package com.example.food.ai.recipe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RecipeGenerateRequest(
        @NotBlank
        @Size(max = 240)
        String ingredients,
        String mealType,
        String goal,
        String searchMode,
        String regenerationPreference,
        String previousTitle,
        DietPreference dietPreference
) {
    public RecipeGenerateRequest(
            String ingredients,
            String mealType,
            String goal,
            String searchMode
    ) {
        this(ingredients, mealType, goal, searchMode, null, null, null);
    }

    public RecipeGenerateRequest(
            String ingredients,
            String mealType,
            String goal,
            String searchMode,
            String regenerationPreference,
            String previousTitle
    ) {
        this(ingredients, mealType, goal, searchMode, regenerationPreference, previousTitle, null);
    }

    public record DietPreference(
            String taste,
            String defaultGoal,
            List<String> avoidIngredients,
            List<String> allergenIngredients
    ) {
    }
}
