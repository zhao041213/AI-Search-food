package com.example.food.recipe.collection.dto;

import java.util.List;

public record SavedRecipePageResponse(
        List<SavedRecipeResponse> items,
        long total,
        int page,
        int size,
        int totalPages
) {
}
