package com.example.food.recipe.collection.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SavedRecipeTagsRequest(
        @NotNull(message = "标签列表不能为空")
        List<String> tags
) {
}
