package com.example.food.suggestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record FeatureSuggestionCreateRequest(
        @NotBlank(message = "建议类型不能为空")
        @Pattern(regexp = "NEW_FEATURE|UX_IMPROVEMENT|BUG_REPORT|OTHER", message = "建议类型不合法")
        String type,
        @NotBlank(message = "建议标题不能为空")
        String title,
        @NotBlank(message = "建议详情不能为空")
        String detail,
        String expectedEffect
) {
}
