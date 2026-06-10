package com.example.food.ai.recipe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecipeGenerateResponse(
        String title,
        String summary,
        List<String> effects,
        List<Ingredient> ingredients,
        List<Step> steps,
        List<String> tips,
        List<String> videoKeywords,
        String provider,
        String model
) {
    public RecipeGenerateResponse {
        effects = effects == null ? List.of() : List.copyOf(effects);
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        steps = steps == null ? List.of() : List.copyOf(steps);
        tips = tips == null ? List.of() : List.copyOf(tips);
        videoKeywords = videoKeywords == null ? List.of() : List.copyOf(videoKeywords);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ingredient(
            String name,
            String amount
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Step(
            int order,
            String title,
            String description,
            Integer durationMinutes
    ) {
    }
}
