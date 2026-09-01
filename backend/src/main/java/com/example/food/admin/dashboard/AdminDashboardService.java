package com.example.food.admin.dashboard;

import com.example.food.admin.dashboard.dto.AdminDashboardDailyTrendResponse;
import com.example.food.admin.dashboard.dto.AdminDashboardHotIngredientResponse;
import com.example.food.admin.dashboard.dto.AdminDashboardInputSourceResponse;
import com.example.food.admin.dashboard.dto.AdminDashboardMetricsResponse;
import com.example.food.admin.dashboard.dto.AdminDashboardOverviewResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardService {

    private static final int HOT_INGREDIENT_LIMIT = 5;

    private final AdminDashboardMapper dashboardMapper;
    private final Clock clock;

    public AdminDashboardService(AdminDashboardMapper dashboardMapper, Clock clock) {
        this.dashboardMapper = dashboardMapper;
        this.clock = clock;
    }

    public AdminDashboardOverviewResponse overview(String periodValue) {
        AdminDashboardPeriod period = AdminDashboardPeriod.parse(periodValue);
        LocalDateTime generatedAt = LocalDateTime.now(clock);
        LocalDate today = generatedAt.toLocalDate();
        LocalDate startDate = period.startDate(today);
        LocalDateTime since = startDate.atStartOfDay();
        LocalDateTime until = period.endExclusive(today);

        return new AdminDashboardOverviewResponse(
                period.value(),
                generatedAt,
                new AdminDashboardMetricsResponse(
                        dashboardMapper.countNewUsers(since, until),
                        dashboardMapper.countGenerations(since, until),
                        dashboardMapper.countSavedRecipes(since, until),
                        dashboardMapper.countReviews(since, until)
                ),
                dailyTrend(startDate, today, since, until),
                inputSources(since, until),
                hotIngredients(since, until)
        );
    }

    private List<AdminDashboardDailyTrendResponse> dailyTrend(
            LocalDate startDate,
            LocalDate today,
            LocalDateTime since,
            LocalDateTime until
    ) {
        Map<LocalDate, Long> generationCounts = countsByDate(dashboardMapper.findDailyGenerations(since, until));
        Map<LocalDate, Long> savedRecipeCounts = countsByDate(dashboardMapper.findDailySavedRecipes(since, until));
        return startDate.datesUntil(today.plusDays(1))
                .map(date -> new AdminDashboardDailyTrendResponse(
                        date,
                        generationCounts.getOrDefault(date, 0L),
                        savedRecipeCounts.getOrDefault(date, 0L)
                ))
                .toList();
    }

    private Map<LocalDate, Long> countsByDate(List<AdminDashboardDailyAggregateRow> rows) {
        Map<LocalDate, Long> counts = new HashMap<>();
        if (rows == null) {
            return counts;
        }
        for (AdminDashboardDailyAggregateRow row : rows) {
            if (row.getTrendDate() != null) {
                counts.put(row.getTrendDate(), row.getCount());
            }
        }
        return counts;
    }

    private List<AdminDashboardInputSourceResponse> inputSources(LocalDateTime since, LocalDateTime until) {
        List<AdminDashboardInputSourceRow> rows = dashboardMapper.findInputSources(since, until);
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .map(row -> new AdminDashboardInputSourceResponse(row.getInputType(), row.getCount()))
                .toList();
    }

    private List<AdminDashboardHotIngredientResponse> hotIngredients(LocalDateTime since, LocalDateTime until) {
        List<AdminDashboardHotIngredientRow> rows = dashboardMapper.findHotIngredients(since, until, HOT_INGREDIENT_LIMIT);
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .map(row -> new AdminDashboardHotIngredientResponse(row.getName(), row.getCount()))
                .toList();
    }
}
