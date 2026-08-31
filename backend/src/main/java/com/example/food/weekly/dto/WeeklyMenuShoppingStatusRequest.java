package com.example.food.weekly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record WeeklyMenuShoppingStatusRequest(
        @NotNull
        LocalDate weekStart,

        @NotBlank
        @Size(max = 128)
        String ingredientName,

        @NotBlank
        @Size(max = 24)
        String status
) {
}
