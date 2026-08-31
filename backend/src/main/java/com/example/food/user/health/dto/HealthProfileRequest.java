package com.example.food.user.health.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record HealthProfileRequest(
        @NotBlank
        @Pattern(regexp = "AGE_18_29|AGE_30_44|AGE_45_59|AGE_60_PLUS")
        String ageRange,

        @NotNull
        @DecimalMin("100.0")
        @DecimalMax("250.0")
        @Digits(integer = 3, fraction = 1)
        BigDecimal heightCm,

        @NotNull
        @DecimalMin("25.0")
        @DecimalMax("300.0")
        @Digits(integer = 3, fraction = 1)
        BigDecimal weightKg,

        @NotBlank
        @Pattern(regexp = "LOW|MODERATE|HIGH")
        String activityLevel
) {
    public HealthProfileRequest {
        ageRange = normalize(ageRange);
        activityLevel = normalize(activityLevel);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
