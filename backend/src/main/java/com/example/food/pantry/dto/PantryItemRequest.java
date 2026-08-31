package com.example.food.pantry.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PantryItemRequest(
        @NotBlank
        @Size(max = 128)
        String ingredientName,
        @Size(max = 64)
        String category,
        @DecimalMin(value = "0.01")
        @Digits(integer = 8, fraction = 2)
        BigDecimal quantity,
        @Size(max = 32)
        String unit,
        LocalDate expireDate
) {
}
