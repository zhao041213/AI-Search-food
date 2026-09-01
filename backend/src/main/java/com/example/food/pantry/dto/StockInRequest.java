package com.example.food.pantry.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockInRequest(
        @NotBlank @Size(max = 32) String sourceType,
        @NotNull Long sourceId,
        @NotBlank @Size(max = 96) String idempotencyKey,
        @NotBlank @Size(max = 128) String ingredientName,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal quantity,
        @NotBlank @Size(max = 32) String unit,
        @Size(max = 64) String category,
        LocalDate expireDate
) {}
