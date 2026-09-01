package com.example.food.recipe.dto;

import java.time.LocalDateTime;

public record RecommendationFeedbackResponse(
        Long searchLogId,
        String reaction,
        boolean cooked,
        LocalDateTime reactedAt,
        LocalDateTime cookedAt
) {
    public static RecommendationFeedbackResponse empty(Long searchLogId) {
        return new RecommendationFeedbackResponse(searchLogId, null, false, null, null);
    }
}
