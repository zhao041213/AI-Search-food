package com.example.food.suggestion.dto;

import java.util.List;

public record FeatureSuggestionPageResponse(
        List<FeatureSuggestionResponse> items,
        long total,
        int page,
        int size,
        int totalPages
) {
}
