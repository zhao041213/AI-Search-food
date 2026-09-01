package com.example.food.admin.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminDashboardMapperIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AdminDashboardMapper dashboardMapper;

    @Test
    void aggregatesDashboardDataWithH2MysqlMode() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        LocalDateTime since = today.minusDays(6).atStartOfDay();
        LocalDateTime until = today.plusDays(1).atStartOfDay();
        LocalDateTime createdAt = today.atTime(10, 30);
        String phone = UUID.randomUUID().toString().replace("-", "");
        String inputType = "dashboard-test";
        String ingredient = "看板测试食材";

        jdbcTemplate.update(
                "INSERT INTO users (phone, nickname, role, enabled, created_at) VALUES (?, ?, 'USER', 1, ?)",
                phone,
                "看板测试用户",
                createdAt
        );
        jdbcTemplate.update(
                "INSERT INTO search_logs (input_type, recognized_ingredients, created_at) VALUES (?, '[]', ?)",
                inputType,
                createdAt
        );
        Long searchLogId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM search_logs", Long.class);
        jdbcTemplate.update(
                "INSERT INTO search_log_ingredients "
                        + "(search_log_id, ingredient_name, original_name, created_at) VALUES (?, ?, ?, ?)",
                searchLogId,
                ingredient,
                ingredient,
                createdAt
        );
        jdbcTemplate.update(
                "INSERT INTO recipe_records (title, created_at) VALUES (?, ?)",
                "看板测试菜谱",
                createdAt
        );
        jdbcTemplate.update(
                """
                INSERT INTO uploaded_files
                    (original_name, stored_name, content_type, file_size, storage_path, purpose, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                "dashboard.jpg",
                "dashboard-test.jpg",
                "image/jpeg",
                1L,
                "test/dashboard.jpg",
                "finished_dish_review",
                createdAt
        );
        Long fileId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM uploaded_files", Long.class);
        jdbcTemplate.update(
                "INSERT INTO finished_dish_reviews (uploaded_file_id, review_result, created_at) VALUES (?, ?, ?)",
                fileId,
                "测试结果",
                createdAt
        );

        assertThat(dashboardMapper.countNewUsers(since, until)).isGreaterThanOrEqualTo(1L);
        assertThat(dashboardMapper.countGenerations(since, until)).isGreaterThanOrEqualTo(1L);
        assertThat(dashboardMapper.countSavedRecipes(since, until)).isGreaterThanOrEqualTo(1L);
        assertThat(dashboardMapper.countReviews(since, until)).isGreaterThanOrEqualTo(1L);
        assertThat(dashboardMapper.findDailyGenerations(since, until))
                .anySatisfy(row -> {
                    assertThat(row.getTrendDate()).isEqualTo(today);
                    assertThat(row.getCount()).isGreaterThanOrEqualTo(1L);
                });
        assertThat(dashboardMapper.findDailySavedRecipes(since, until))
                .anySatisfy(row -> {
                    assertThat(row.getTrendDate()).isEqualTo(today);
                    assertThat(row.getCount()).isGreaterThanOrEqualTo(1L);
                });
        assertThat(dashboardMapper.findInputSources(since, until))
                .anySatisfy(row -> {
                    assertThat(row.getInputType()).isEqualTo(inputType);
                    assertThat(row.getCount()).isEqualTo(1L);
                });
        assertThat(dashboardMapper.findHotIngredients(since, until, 50))
                .anySatisfy(row -> {
                    assertThat(row.getName()).isEqualTo(ingredient);
                    assertThat(row.getCount()).isEqualTo(1L);
                });
    }
}
