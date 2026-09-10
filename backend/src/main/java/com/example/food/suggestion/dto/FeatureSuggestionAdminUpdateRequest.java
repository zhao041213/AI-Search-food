package com.example.food.suggestion.dto;

import jakarta.validation.constraints.Pattern;

public record FeatureSuggestionAdminUpdateRequest(
        @Pattern(regexp = "PENDING|ACCEPTED|PLANNED|IN_DEVELOPMENT|COMPLETED|DECLINED", message = "建议状态不合法")
        String status,
        String adminReply,
        String internalNote,
        Long duplicateOfId
) {
}
