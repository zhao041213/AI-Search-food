package com.example.food.recipe;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PersistenceSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void recipeHistoryColumnsAreCreatedByFlyway() {
        assertThat(columnExists("search_logs", "meal_type")).isTrue();
        assertThat(columnExists("search_logs", "goal")).isTrue();
        assertThat(columnExists("recipe_records", "summary")).isTrue();
        assertThat(columnExists("recipe_records", "effects")).isTrue();
        assertThat(columnExists("recipe_records", "tips")).isTrue();
    }

    @Test
    void searchLogIngredientDetailsAreCreatedByFlyway() {
        assertThat(tableExists("search_log_ingredients")).isTrue();
        assertThat(columnExists("search_log_ingredients", "search_log_id")).isTrue();
        assertThat(columnExists("search_log_ingredients", "ingredient_name")).isTrue();
        assertThat(columnExists("search_log_ingredients", "original_name")).isTrue();
        assertThat(columnExists("search_log_ingredients", "created_at")).isTrue();
    }

    @Test
    void dietPreferenceTableAndSearchHistoryIndexAreCreatedByFlyway() {
        assertThat(tableExists("user_diet_preferences")).isTrue();
        assertThat(columnExists("user_diet_preferences", "user_id")).isTrue();
        assertThat(columnExists("user_diet_preferences", "taste")).isTrue();
        assertThat(columnExists("user_diet_preferences", "default_goal")).isTrue();
        assertThat(columnExists("user_diet_preferences", "avoid_ingredients")).isTrue();
        assertThat(columnExists("user_diet_preferences", "allergen_ingredients")).isTrue();
        assertThat(columnExists("user_diet_preferences", "created_at")).isTrue();
        assertThat(columnExists("user_diet_preferences", "updated_at")).isTrue();
        assertThat(indexExists("idx_search_logs_user_created")).isTrue();
    }

    @Test
    void ingredientImageCacheTableIsCreatedByFlyway() {
        assertThat(tableExists("ingredient_images")).isTrue();
        assertThat(columnExists("ingredient_images", "canonical_name")).isTrue();
        assertThat(columnExists("ingredient_images", "image_data")).isTrue();
        assertThat(columnExists("ingredient_images", "verification_status")).isTrue();
        assertThat(columnExists("ingredient_images", "source_url")).isTrue();
    }

    @Test
    void shoppingItemCheckTableIsCreatedByFlyway() {
        assertThat(tableExists("shopping_item_checks")).isTrue();
        assertThat(columnExists("shopping_item_checks", "user_id")).isTrue();
        assertThat(columnExists("shopping_item_checks", "search_log_id")).isTrue();
        assertThat(columnExists("shopping_item_checks", "ingredient_name")).isTrue();
        assertThat(columnExists("shopping_item_checks", "status")).isTrue();
        assertThat(columnExists("shopping_item_checks", "checked")).isTrue();
    }

    @Test
    void healthProfileTableIsCreatedByFlyway() {
        assertThat(tableExists("user_health_profiles")).isTrue();
        assertThat(columnExists("user_health_profiles", "user_id")).isTrue();
        assertThat(columnExists("user_health_profiles", "age_range")).isTrue();
        assertThat(columnExists("user_health_profiles", "height_cm")).isTrue();
        assertThat(columnExists("user_health_profiles", "weight_kg")).isTrue();
        assertThat(columnExists("user_health_profiles", "activity_level")).isTrue();
        assertThat(columnExists("user_health_profiles", "created_at")).isTrue();
        assertThat(columnExists("user_health_profiles", "updated_at")).isTrue();
    }

    @Test
    void recommendationFeedbackTableAndSearchResultSnapshotsAreCreatedByFlyway() {
        assertThat(columnExists("search_logs", "result_title")).isTrue();
        assertThat(columnExists("search_logs", "result_ingredients")).isTrue();
        assertThat(tableExists("recommendation_feedbacks")).isTrue();
        assertThat(columnExists("recommendation_feedbacks", "user_id")).isTrue();
        assertThat(columnExists("recommendation_feedbacks", "search_log_id")).isTrue();
        assertThat(columnExists("recommendation_feedbacks", "reaction")).isTrue();
        assertThat(columnExists("recommendation_feedbacks", "cooked")).isTrue();
        assertThat(columnExists("recommendation_feedbacks", "reacted_at")).isTrue();
        assertThat(columnExists("recommendation_feedbacks", "cooked_at")).isTrue();
        assertThat(indexExists("idx_recommendation_feedback_user_updated")).isTrue();
    }

    @Test
    void dietPreferenceIsDeletedWithItsUser() {
        jdbcTemplate.update("""
                INSERT INTO users (phone, nickname)
                VALUES (?, ?)
                """, "13800138001", "diet-preference-test");
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE phone = ?",
                Long.class,
                "13800138001"
        );
        jdbcTemplate.update("""
                INSERT INTO user_diet_preferences (
                    user_id, taste, default_goal, avoid_ingredients, allergen_ingredients
                ) VALUES (?, ?, ?, ?, ?)
                """, userId, "light", "balanced", "[]", "[]");

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_diet_preferences WHERE user_id = ?",
                Integer.class,
                userId
        );
        assertThat(count).isZero();
    }

    @Test
    void healthProfileIsDeletedWithItsUser() {
        jdbcTemplate.update("""
                INSERT INTO users (phone, nickname)
                VALUES (?, ?)
                """, "13800138002", "health-profile-test");
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE phone = ?",
                Long.class,
                "13800138002"
        );
        jdbcTemplate.update("""
                INSERT INTO user_health_profiles (
                    user_id, age_range, height_cm, weight_kg, activity_level
                ) VALUES (?, ?, ?, ?, ?)
                """, userId, "AGE_30_44", 172.0, 64.0, "MODERATE");

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_health_profiles WHERE user_id = ?",
                Integer.class,
                userId
        );
        assertThat(count).isZero();
    }

    @Test
    void recommendationFeedbackIsDeletedWithItsUserAndSearchLog() {
        jdbcTemplate.update("INSERT INTO users (phone, nickname) VALUES (?, ?)", "13800138011", "feedback-cascade-test");
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE phone = ?", Long.class, "13800138011");
        jdbcTemplate.update(
                "INSERT INTO search_logs (user_id, input_type, query_text) VALUES (?, 'text', ?)",
                userId, "番茄"
        );
        Long searchLogId = jdbcTemplate.queryForObject(
                "SELECT id FROM search_logs WHERE user_id = ? AND query_text = ?",
                Long.class, userId, "番茄");
        jdbcTemplate.update(
                "INSERT INTO recommendation_feedbacks (user_id, search_log_id, reaction, cooked) VALUES (?, ?, 'LIKE', 1)",
                userId, searchLogId
        );

        jdbcTemplate.update("DELETE FROM search_logs WHERE id = ?", searchLogId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_feedbacks WHERE search_log_id = ?",
                Integer.class, searchLogId
        )).isZero();

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_feedbacks WHERE user_id = ?",
                Integer.class, userId
        )).isZero();
    }

    @Test
    void weeklyMenuTablesAreCreatedByFlyway() {
        assertThat(tableExists("weekly_menu_plans")).isTrue();
        assertThat(columnExists("weekly_menu_plans", "user_id")).isTrue();
        assertThat(columnExists("weekly_menu_plans", "week_start_date")).isTrue();
        assertThat(tableExists("weekly_menu_items")).isTrue();
        assertThat(columnExists("weekly_menu_items", "plan_id")).isTrue();
        assertThat(columnExists("weekly_menu_items", "menu_date")).isTrue();
        assertThat(columnExists("weekly_menu_items", "meal_type")).isTrue();
        assertThat(columnExists("weekly_menu_items", "recipe_id")).isTrue();
        assertThat(tableExists("weekly_menu_shopping_checks")).isTrue();
        assertThat(columnExists("weekly_menu_shopping_checks", "ingredient_name")).isTrue();
        assertThat(columnExists("weekly_menu_shopping_checks", "status")).isTrue();
    }

    @Test
    void notificationTablesAreCreatedByFlyway() {
        assertThat(tableExists("user_notifications")).isTrue();
        assertThat(columnExists("user_notifications", "user_id")).isTrue();
        assertThat(columnExists("user_notifications", "notification_type")).isTrue();
        assertThat(columnExists("user_notifications", "dedupe_key")).isTrue();
        assertThat(columnExists("user_notifications", "status")).isTrue();
        assertThat(columnExists("user_notifications", "target_path")).isTrue();
        assertThat(tableExists("user_notification_preferences")).isTrue();
        assertThat(columnExists("user_notification_preferences", "pantry_expiring_enabled")).isTrue();
        assertThat(columnExists("user_notification_preferences", "pantry_expired_enabled")).isTrue();
        assertThat(columnExists("user_notification_preferences", "weekly_menu_preparation_enabled")).isTrue();
        assertThat(tableExists("notification_scan_runs")).isTrue();
        assertThat(columnExists("notification_scan_runs", "finished_at")).isTrue();
        assertThat(columnExists("notification_scan_runs", "error_summary")).isTrue();
        assertThat(indexExists("idx_user_notifications_user_status_created")).isTrue();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE LOWER(TABLE_NAME) = ?",
                Integer.class,
                tableName
        );
        return count != null && count == 1;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE LOWER(TABLE_NAME) = ? AND LOWER(COLUMN_NAME) = ?",
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count == 1;
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE LOWER(INDEX_NAME) = ?",
                Integer.class,
                indexName
        );
        return count != null && count == 1;
    }
}
