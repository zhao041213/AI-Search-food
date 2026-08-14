package com.example.food.user.preference.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DietPreferenceRequest(
        @Pattern(regexp = "any|light|home|spicy|sweet_sour")
        String taste,

        @Pattern(regexp = "balanced|fat_loss|muscle_gain|low_sugar")
        String defaultGoal,

        @Size(max = 20)
        List<@Size(max = 32) String> avoidIngredients,

        @Size(max = 20)
        List<@Size(max = 32) String> allergenIngredients
) {
    public DietPreferenceRequest {
        taste = trimToNull(taste);
        defaultGoal = trimToNull(defaultGoal);
        avoidIngredients = trimIngredients(avoidIngredients);
        allergenIngredients = trimIngredients(allergenIngredients);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static List<String> trimIngredients(List<String> ingredients) {
        if (ingredients == null) {
            return null;
        }
        return ingredients.stream()
                .map(value -> value == null ? null : value.trim())
                .toList();
    }
}
