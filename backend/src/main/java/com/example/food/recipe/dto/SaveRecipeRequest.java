package com.example.food.recipe.dto;

import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaveRecipeRequest(
        @NotNull
        Long searchLogId,
        @NotBlank
        @Size(max = 128)
        String title,
        String summary,
        List<String> effects,
        @NotEmpty
        @Valid List<RecipeGenerateResponse.Ingredient> ingredients,
        List<RecipeGenerateResponse.MissingIngredient> missingIngredients,
        @NotEmpty
        @Valid List<RecipeGenerateResponse.Step> steps,
        List<String> tips,
        List<String> videoKeywords,
        RecipeGenerateResponse.Explanation explanation,
        @NotBlank
        @Size(max = 64)
        String provider,
        @NotBlank
        @Size(max = 128)
        String model
) {
    public SaveRecipeRequest {
        effects = effects == null ? List.of() : List.copyOf(effects);
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        missingIngredients = missingIngredients == null ? List.of() : List.copyOf(missingIngredients);
        steps = steps == null ? List.of() : List.copyOf(steps);
        tips = tips == null ? List.of() : List.copyOf(tips);
        videoKeywords = videoKeywords == null ? List.of() : List.copyOf(videoKeywords);
        explanation = explanation == null ? RecipeGenerateResponse.Explanation.empty() : explanation;
    }

    public SaveRecipeRequest(
            Long searchLogId,
            String title,
            String summary,
            List<String> effects,
            List<RecipeGenerateResponse.Ingredient> ingredients,
            List<RecipeGenerateResponse.Step> steps,
            List<String> tips,
            List<String> videoKeywords,
            String provider,
            String model
    ) {
        this(
                searchLogId,
                title,
                summary,
                effects,
                ingredients,
                List.of(),
                steps,
                tips,
                videoKeywords,
                RecipeGenerateResponse.Explanation.empty(),
                provider,
                model
        );
    }
}
