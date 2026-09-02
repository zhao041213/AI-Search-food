package com.example.food.pantry.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PantryReadinessRequest(
        @NotEmpty @Size(max = 30) List<@Valid Ingredient> ingredients
) {
    public record Ingredient(
            @NotBlank @Size(max = 128) String name,
            @Size(max = 64) String amount
    ) {
    }
}
