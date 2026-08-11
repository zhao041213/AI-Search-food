package com.example.food.stats;

public class HotIngredientTotals {

    private long totalSearches;
    private long totalIngredientOccurrences;

    public HotIngredientTotals() {
    }

    public HotIngredientTotals(long totalSearches, long totalIngredientOccurrences) {
        this.totalSearches = totalSearches;
        this.totalIngredientOccurrences = totalIngredientOccurrences;
    }

    public long getTotalSearches() {
        return totalSearches;
    }

    public void setTotalSearches(long totalSearches) {
        this.totalSearches = totalSearches;
    }

    public long getTotalIngredientOccurrences() {
        return totalIngredientOccurrences;
    }

    public void setTotalIngredientOccurrences(long totalIngredientOccurrences) {
        this.totalIngredientOccurrences = totalIngredientOccurrences;
    }
}
