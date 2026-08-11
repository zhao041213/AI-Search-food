package com.example.food.stats;

import com.example.food.stats.dto.HotIngredientStatsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HotIngredientStatsIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private HotIngredientStatsService service;

    @Test
    void aggregatesAllSevenDayAndThirtyDayWindows() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        insertSearch(now.minusDays(1), "番茄", "鸡蛋");
        insertSearch(now.minusDays(8), "番茄");
        insertSearch(now.minusDays(40), "豆腐");

        HotIngredientStatsResponse sevenDays = service.get("7d", 10);
        HotIngredientStatsResponse thirtyDays = service.get("30d", 10);
        HotIngredientStatsResponse all = service.get("all", 10);

        assertThat(counts(sevenDays)).containsExactlyInAnyOrderEntriesOf(Map.of("番茄", 1L, "鸡蛋", 1L));
        assertThat(sevenDays.totalSearches()).isEqualTo(1);
        assertThat(sevenDays.totalIngredientOccurrences()).isEqualTo(2);
        assertThat(counts(thirtyDays)).containsEntry("番茄", 2L).containsEntry("鸡蛋", 1L);
        assertThat(thirtyDays.totalSearches()).isEqualTo(2);
        assertThat(counts(all)).containsEntry("豆腐", 1L);
        assertThat(all.totalSearches()).isEqualTo(3);
    }

    private void insertSearch(LocalDateTime createdAt, String... ingredients) {
        jdbcTemplate.update(
                "INSERT INTO search_logs (input_type, recognized_ingredients, created_at) VALUES ('text', '[]', ?)",
                createdAt
        );
        Long searchLogId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM search_logs", Long.class);
        for (String ingredient : ingredients) {
            jdbcTemplate.update(
                    "INSERT INTO search_log_ingredients "
                            + "(search_log_id, ingredient_name, original_name, created_at) VALUES (?, ?, ?, ?)",
                    searchLogId,
                    ingredient,
                    ingredient,
                    createdAt
            );
        }
    }

    private Map<String, Long> counts(HotIngredientStatsResponse response) {
        return response.items().stream().collect(Collectors.toMap(
                item -> item.name(),
                item -> item.searchCount()
        ));
    }
}
