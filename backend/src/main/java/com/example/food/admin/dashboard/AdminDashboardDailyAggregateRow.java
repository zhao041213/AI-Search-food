package com.example.food.admin.dashboard;

import java.time.LocalDate;

public class AdminDashboardDailyAggregateRow {

    private LocalDate trendDate;
    private long count;

    public LocalDate getTrendDate() {
        return trendDate;
    }

    public void setTrendDate(LocalDate trendDate) {
        this.trendDate = trendDate;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
