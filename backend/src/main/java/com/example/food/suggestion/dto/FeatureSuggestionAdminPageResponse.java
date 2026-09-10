package com.example.food.suggestion.dto;

import java.util.List;

public record FeatureSuggestionAdminPageResponse(
        List<FeatureSuggestionAdminResponse> items,
        long total,
        int page,
        int size,
        int totalPages
) {
}
