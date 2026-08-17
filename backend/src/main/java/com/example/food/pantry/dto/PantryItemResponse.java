package com.example.food.pantry.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PantryItemResponse(
        Long id,
        String ingredientName,
        String category,
        BigDecimal quantity,
        String unit,
        LocalDate expireDate,
        LocalDateTime updatedAt
) {
}
