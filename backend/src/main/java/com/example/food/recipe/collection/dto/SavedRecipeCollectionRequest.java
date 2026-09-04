package com.example.food.recipe.collection.dto;

import jakarta.validation.constraints.NotNull;

public record SavedRecipeCollectionRequest(
        @NotNull(message = "收藏夹不能为空")
        Long collectionId
) {
}
