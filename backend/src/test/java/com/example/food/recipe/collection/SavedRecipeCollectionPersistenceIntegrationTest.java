package com.example.food.recipe.collection;

import com.example.food.recipe.collection.dto.RecipeCollectionRequest;
import com.example.food.recipe.collection.dto.RecipeCollectionResponse;
import com.example.food.recipe.collection.dto.SavedRecipeCollectionRequest;
import com.example.food.recipe.collection.dto.SavedRecipePageResponse;
import com.example.food.recipe.collection.dto.SavedRecipeTagsRequest;
import com.example.food.security.UserSecurityLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SavedRecipeCollectionPersistenceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SavedRecipeCollectionService service;

    @Test
    void createsDefaultAndCustomCollectionsMovesTagsAndAuditsChanges() {
        Long userId = insertUser("13900000201");
        Long recipeId = insertRecipe(userId, "番茄炒蛋", "番茄、鸡蛋");

        List<RecipeCollectionResponse> initialCollections = service.listCollections(userId);
        RecipeCollectionResponse defaultCollection = initialCollections.stream()
                .filter(RecipeCollectionResponse::defaultCollection)
                .findFirst()
                .orElseThrow();
        assertThat(defaultCollection.recipeCount()).isEqualTo(1);

        RecipeCollectionResponse custom = service.createCollection(userId, new RecipeCollectionRequest("晚餐"));
        service.moveRecipe(userId, recipeId, new SavedRecipeCollectionRequest(custom.id()));
        service.replaceTags(userId, recipeId, new SavedRecipeTagsRequest(List.of("家常", "高蛋白")));

        SavedRecipePageResponse moved = service.listSavedRecipes(
                userId, custom.id(), "鸡蛋", "", "", "家常", "savedAtDesc", 1, 20);
        assertThat(moved.total()).isEqualTo(1);
        assertThat(moved.items()).singleElement().satisfies(item -> {
            assertThat(item.collectionName()).isEqualTo("晚餐");
            assertThat(item.tags()).containsExactly("家常", "高蛋白");
        });
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_security_logs WHERE user_id = ? AND action_type = ?",
                Integer.class,
                userId,
                UserSecurityLogService.COLLECTION_CHANGE
        )).isEqualTo(3);
    }

    private Long insertUser(String phone) {
        jdbcTemplate.update(
                "INSERT INTO users (phone, nickname, role, enabled) VALUES (?, ?, 'USER', 1)",
                phone,
                "收藏测试用户"
        );
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE phone = ?", Long.class, phone);
    }

    private Long insertRecipe(Long userId, String title, String ingredients) {
        jdbcTemplate.update(
                "INSERT INTO recipe_records (user_id, title, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)",
                userId,
                title
        );
        Long recipeId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM recipe_records WHERE user_id = ?", Long.class, userId);
        jdbcTemplate.update(
                "INSERT INTO recipe_ingredients (recipe_id, ingredient_name) VALUES (?, ?)",
                recipeId,
                ingredients
        );
        return recipeId;
    }
}
