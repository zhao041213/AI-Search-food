package com.example.food.recipe.share.dto;

import java.math.BigDecimal;
import java.util.List;

public record PublicRecipeResponse(
        String title,
        String summary,
        List<Ingredient> ingredients,
        List<Step> steps,
        List<String> tips,
        Nutrition nutrition
) {

    public record Ingredient(String name, String amount) {
    }

    public record Step(int order, String title, String description, Integer durationMinutes) {
    }

    public record Nutrition(
            Integer servings,
            BigDecimal caloriesKcal,
            BigDecimal proteinG,
            BigDecimal fatG,
            BigDecimal carbohydrateG
    ) {
    }
}
