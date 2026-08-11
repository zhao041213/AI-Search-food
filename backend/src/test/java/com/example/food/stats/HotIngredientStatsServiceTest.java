package com.example.food.stats;

import com.example.food.stats.dto.HotIngredientStatsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotIngredientStatsServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 11, 12, 0);

    @Mock
    private SearchLogIngredientMapper ingredientMapper;

    private HotIngredientStatsService service;

    @BeforeEach
    void setUp() {
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        Clock clock = Clock.fixed(NOW.atZone(zone).toInstant(), zone);
        service = new HotIngredientStatsService(ingredientMapper, clock);
    }

    @Test
    void returnsRankedItemsAndOccurrenceShareForThirtyDays() {
        LocalDateTime latest = LocalDateTime.of(2026, 8, 10, 18, 20);
        when(ingredientMapper.summarize(NOW.minusDays(30)))
                .thenReturn(new HotIngredientTotals(6, 10));
        when(ingredientMapper.findHotIngredients(NOW.minusDays(30), 10))
                .thenReturn(List.of(
                        new HotIngredientAggregateRow("番茄", 4, latest),
                        new HotIngredientAggregateRow("鸡蛋", 3, latest.minusHours(1))
                ));

        HotIngredientStatsResponse response = service.get("30d", 10);

        assertThat(response.period()).isEqualTo("30d");
        assertThat(response.totalSearches()).isEqualTo(6);
        assertThat(response.totalIngredientOccurrences()).isEqualTo(10);
        assertThat(response.generatedAt()).isEqualTo(NOW);
        assertThat(response.items())
                .extracting("rank", "name", "searchCount", "share")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, "番茄", 4L, 0.4d),
                        org.assertj.core.groups.Tuple.tuple(2, "鸡蛋", 3L, 0.3d)
                );
    }

    @Test
    void allPeriodUsesNoCutoffAndSevenDaysUsesSevenDayCutoff() {
        when(ingredientMapper.summarize(null)).thenReturn(new HotIngredientTotals(0, 0));
        when(ingredientMapper.findHotIngredients(null, 5)).thenReturn(List.of());

        service.get("all", 5);

        verify(ingredientMapper).summarize(null);
        when(ingredientMapper.summarize(NOW.minusDays(7))).thenReturn(new HotIngredientTotals(0, 0));
        when(ingredientMapper.findHotIngredients(NOW.minusDays(7), 5)).thenReturn(List.of());

        service.get("7d", 5);

        verify(ingredientMapper).summarize(NOW.minusDays(7));
    }

    @Test
    void rejectsUnsupportedPeriodAndOutOfRangeLimit() {
        assertThatThrownBy(() -> service.get("year", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("统计周期仅支持 all、7d、30d");
        assertThatThrownBy(() -> service.get("all", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("排行榜数量必须在 1 到 50 之间");
        assertThatThrownBy(() -> service.get("all", 51))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("排行榜数量必须在 1 到 50 之间");
    }
}
