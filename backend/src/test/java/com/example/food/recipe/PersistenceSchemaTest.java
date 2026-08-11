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
}
