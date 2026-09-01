package com.example.food.pantry.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.util.List;

public record CookingConsumptionRequest(
        @NotNull Long recipeId,
        @Min(1) @Max(20) Integer actualServings,
        @NotBlank @Size(max = 96) String idempotencyKey,
        @Valid List<Item> items
) {
    public record Item(
            @NotBlank @Size(max = 128) String ingredientName,
            @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal quantity,
            @Size(max = 32) String unit,
            Boolean selected
    ) {}
}
