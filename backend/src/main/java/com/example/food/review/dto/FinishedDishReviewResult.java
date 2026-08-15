package com.example.food.review.dto;

import java.util.List;

public record FinishedDishReviewResult(
        String recipeTitle,
        int overallScore,
        Dimension color,
        Dimension doneness,
        Dimension plating,
        String summary,
        List<String> strengths,
        List<Issue> issues,
        String safetyNote,
        String provider,
        String model
) {

    public record Dimension(int score, String comment) {
    }

    public record Issue(String severity, String title, String evidence, String suggestion) {
    }
}
