package com.example.food.user.healthnutrition.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record HealthNutritionProfileResponse(
        boolean configured,
        String gender,
        Integer age,
        BigDecimal heightCm,
        BigDecimal weightKg,
        BigDecimal bmi,
        String activityLevel,
        String goal,
        List<String> dietaryRestrictions,
        List<String> allergies,
        String specialHealthCondition,
        String targetMode,
        NutritionValues aiReference,
        NutritionValues currentTarget,
        String aiReferenceBasis,
        LocalDateTime aiReferenceUpdatedAt,
        LocalDateTime updatedAt,
        boolean generalDietaryReferenceOnly
) {
    public static HealthNutritionProfileResponse empty() {
        return new HealthNutritionProfileResponse(false, null, null, null, null, null, null, null,
                List.of(), List.of(), null, "DISABLED", null, null, null, null, null, false);
    }
}
