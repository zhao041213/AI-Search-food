package com.example.food.admin.dashboard;

import com.example.food.admin.dashboard.dto.AdminDashboardOverviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 31, 12, 0);

    @Mock
    private AdminDashboardMapper dashboardMapper;

    private AdminDashboardService service;

    @BeforeEach
    void setUp() {
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        Clock clock = Clock.fixed(NOW.atZone(zone).toInstant(), zone);
        service = new AdminDashboardService(dashboardMapper, clock);
    }

    @Test
    void returnsAggregatedOverviewAndFillsEachDayInSelectedPeriod() {
        LocalDateTime since = LocalDateTime.of(2026, 8, 25, 0, 0);
        LocalDateTime until = LocalDateTime.of(2026, 9, 1, 0, 0);
        when(dashboardMapper.countNewUsers(since, until)).thenReturn(3L);
        when(dashboardMapper.countGenerations(since, until)).thenReturn(9L);
        when(dashboardMapper.countSavedRecipes(since, until)).thenReturn(4L);
        when(dashboardMapper.countReviews(since, until)).thenReturn(2L);
        when(dashboardMapper.findDailyGenerations(since, until)).thenReturn(List.of(
                dailyRow(LocalDate.of(2026, 8, 25), 2),
                dailyRow(LocalDate.of(2026, 8, 31), 1)
        ));
        when(dashboardMapper.findDailySavedRecipes(since, until)).thenReturn(List.of(
                dailyRow(LocalDate.of(2026, 8, 30), 4)
        ));
        when(dashboardMapper.findInputSources(since, until)).thenReturn(List.of(
                inputSourceRow("text", 6),
                inputSourceRow("camera", 3)
        ));
        when(dashboardMapper.findHotIngredients(since, until, 5)).thenReturn(List.of(
                hotIngredientRow("番茄", 5),
                hotIngredientRow("鸡蛋", 4)
        ));

        AdminDashboardOverviewResponse response = service.overview("7d");

        assertThat(response.period()).isEqualTo("7d");
        assertThat(response.generatedAt()).isEqualTo(NOW);
        assertThat(response.metrics().newUserCount()).isEqualTo(3);
        assertThat(response.metrics().generationCount()).isEqualTo(9);
        assertThat(response.metrics().savedRecipeCount()).isEqualTo(4);
        assertThat(response.metrics().reviewCount()).isEqualTo(2);
        assertThat(response.dailyTrend())
                .extracting("date", "generationCount", "savedRecipeCount")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 8, 25), 2L, 0L),
                        org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 8, 26), 0L, 0L),
                        org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 8, 27), 0L, 0L),
                        org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 8, 28), 0L, 0L),
                        org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 8, 29), 0L, 0L),
                        org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 8, 30), 0L, 4L),
                        org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 8, 31), 1L, 0L)
                );
        assertThat(response.inputSources())
                .extracting("inputType", "count")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("text", 6L),
                        org.assertj.core.groups.Tuple.tuple("camera", 3L)
                );
        assertThat(response.hotIngredients())
                .extracting("name", "count")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("番茄", 5L),
                        org.assertj.core.groups.Tuple.tuple("鸡蛋", 4L)
                );
        verify(dashboardMapper).findHotIngredients(eq(since), eq(until), eq(5));
    }

    @Test
    void supportsThirtyDayPeriodAndRejectsUnsupportedPeriod() {
        LocalDateTime since = LocalDateTime.of(2026, 8, 2, 0, 0);
        LocalDateTime until = LocalDateTime.of(2026, 9, 1, 0, 0);
        when(dashboardMapper.findDailyGenerations(since, until)).thenReturn(List.of());
        when(dashboardMapper.findDailySavedRecipes(since, until)).thenReturn(List.of());
        when(dashboardMapper.findInputSources(since, until)).thenReturn(List.of());
        when(dashboardMapper.findHotIngredients(since, until, 5)).thenReturn(List.of());

        AdminDashboardOverviewResponse response = service.overview("30d");

        assertThat(response.period()).isEqualTo("30d");
        assertThat(response.dailyTrend()).hasSize(30);
        assertThat(response.dailyTrend().get(0).date()).isEqualTo(LocalDate.of(2026, 8, 2));
        assertThat(response.dailyTrend().get(response.dailyTrend().size() - 1).date())
                .isEqualTo(LocalDate.of(2026, 8, 31));
        assertThatThrownBy(() -> service.overview("all"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("统计周期仅支持 7d 或 30d");
    }

    private AdminDashboardDailyAggregateRow dailyRow(LocalDate date, long count) {
        AdminDashboardDailyAggregateRow row = new AdminDashboardDailyAggregateRow();
        row.setTrendDate(date);
        row.setCount(count);
        return row;
    }

    private AdminDashboardInputSourceRow inputSourceRow(String inputType, long count) {
        AdminDashboardInputSourceRow row = new AdminDashboardInputSourceRow();
        row.setInputType(inputType);
        row.setCount(count);
        return row;
    }

    private AdminDashboardHotIngredientRow hotIngredientRow(String name, long count) {
        AdminDashboardHotIngredientRow row = new AdminDashboardHotIngredientRow();
        row.setName(name);
        row.setCount(count);
        return row;
    }
}
