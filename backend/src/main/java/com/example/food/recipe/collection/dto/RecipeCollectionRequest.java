package com.example.food.recipe.collection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecipeCollectionRequest(
        @NotBlank(message = "收藏夹名称不能为空")
        @Size(max = 64, message = "收藏夹名称不能超过64个字符")
        String name
) {
}
