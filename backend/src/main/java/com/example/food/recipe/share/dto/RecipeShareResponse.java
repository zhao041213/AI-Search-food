package com.example.food.recipe.share.dto;

import java.time.LocalDateTime;

public record RecipeShareResponse(
        Long id,
        Long recipeId,
        String recipeTitle,
        String token,
        String shareUrl,
        String validity,
        LocalDateTime expiresAt,
        LocalDateTime disabledAt,
        String status,
        LocalDateTime createdAt
) {
}
