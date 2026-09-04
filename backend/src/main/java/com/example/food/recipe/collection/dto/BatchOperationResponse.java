package com.example.food.recipe.collection.dto;

import java.util.List;

public record BatchOperationResponse(
        int successCount,
        int failureCount,
        List<BatchOperationFailure> failures
) {
}
