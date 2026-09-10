package com.example.food.suggestion.dto;

import jakarta.validation.constraints.NotBlank;

public record FeatureSuggestionAppendRequest(
        @NotBlank(message = "补充内容不能为空")
        String content
) {
}
