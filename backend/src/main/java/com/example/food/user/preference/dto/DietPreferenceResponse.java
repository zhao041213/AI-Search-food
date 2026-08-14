package com.example.food.user.preference.dto;

import java.util.List;

public record DietPreferenceResponse(
        String taste,
        String defaultGoal,
        List<String> avoidIngredients,
        List<String> allergenIngredients
) {
    public static DietPreferenceResponse empty() {
        return new DietPreferenceResponse("any", "balanced", List.of(), List.of());
    }
}
