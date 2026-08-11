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
        @NotEmpty
        @Valid List<RecipeGenerateResponse.Step> steps,
        List<String> tips,
        List<String> videoKeywords,
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
        steps = steps == null ? List.of() : List.copyOf(steps);
        tips = tips == null ? List.of() : List.copyOf(tips);
        videoKeywords = videoKeywords == null ? List.of() : List.copyOf(videoKeywords);
    }
}
