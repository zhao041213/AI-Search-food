package com.example.food.stats;

import java.time.LocalDateTime;

public class HotIngredientAggregateRow {

    private String ingredientName;
    private long searchCount;
    private LocalDateTime latestSearchAt;

    public HotIngredientAggregateRow() {
    }

    public HotIngredientAggregateRow(String ingredientName, long searchCount, LocalDateTime latestSearchAt) {
        this.ingredientName = ingredientName;
        this.searchCount = searchCount;
        this.latestSearchAt = latestSearchAt;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public long getSearchCount() {
        return searchCount;
    }

    public void setSearchCount(long searchCount) {
        this.searchCount = searchCount;
    }

    public LocalDateTime getLatestSearchAt() {
        return latestSearchAt;
    }

    public void setLatestSearchAt(LocalDateTime latestSearchAt) {
        this.latestSearchAt = latestSearchAt;
    }
}
