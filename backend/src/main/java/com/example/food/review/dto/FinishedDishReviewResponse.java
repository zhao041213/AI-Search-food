package com.example.food.review.dto;

import java.time.LocalDateTime;

public record FinishedDishReviewResponse(
        Long id,
        Long recipeId,
        FinishedDishReviewResult result,
        boolean saved,
        LocalDateTime createdAt
) {
}
