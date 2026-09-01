package com.example.food.weekly.dto;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public record WeeklyMenuResponse(
        Long id,
        LocalDate weekStart,
        LocalDate weekEnd,
        List<WeeklyMenuItemResponse> items,
        List<WeeklyShoppingItemResponse> shoppingItems,
        NutritionSummary nutritionSummary
) {
    public WeeklyMenuResponse {
        items = items == null ? List.of() : List.copyOf(items);
        shoppingItems = shoppingItems == null ? List.of() : List.copyOf(shoppingItems);
        nutritionSummary = nutritionSummary == null ? NutritionSummary.empty(weekStart) : nutritionSummary;
    }

    public WeeklyMenuResponse(
            Long id,
            LocalDate weekStart,
            LocalDate weekEnd,
            List<WeeklyMenuItemResponse> items,
            List<WeeklyShoppingItemResponse> shoppingItems
    ) {
        this(id, weekStart, weekEnd, items, shoppingItems, NutritionSummary.empty(weekStart));
    }

    public record NutritionSummary(
            int assignedMealCount,
            int validEstimateMealCount,
            List<DailyNutritionSummary> daily,
            NutritionTotals weekly
    ) {
        public NutritionSummary {
            daily = daily == null ? List.of() : List.copyOf(daily);
        }

        public static NutritionSummary empty(LocalDate weekStart) {
            return new NutritionSummary(0, 0, List.of(), null);
        }
    }

    public record DailyNutritionSummary(
            LocalDate date,
            int assignedMealCount,
            int validEstimateMealCount,
            NutritionTotals totals
    ) {
    }

    public record NutritionTotals(
            BigDecimal caloriesKcal,
            BigDecimal proteinG,
            BigDecimal fatG,
            BigDecimal carbohydrateG
    ) {
    }
}
