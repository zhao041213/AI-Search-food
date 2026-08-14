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
