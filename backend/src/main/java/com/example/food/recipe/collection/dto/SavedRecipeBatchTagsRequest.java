package com.example.food.recipe.collection.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SavedRecipeBatchTagsRequest(
        @NotEmpty(message = "请选择至少一道菜谱")
        @Size(max = 100, message = "单次最多操作100道菜谱")
        List<Long> recipeIds,
        @NotNull(message = "待添加标签不能为空")
        List<String> addTags,
        @NotNull(message = "待移除标签不能为空")
        List<String> removeTags
) {
}
