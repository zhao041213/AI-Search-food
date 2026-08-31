package com.example.food.pantry.dto;

import java.time.LocalDate;
import java.util.List;

public record PantryExpirySummaryResponse(
        LocalDate asOf,
        int warningDays,
        List<PantryItemResponse> expiredItems,
        List<PantryItemResponse> expiringSoonItems
) {
}
