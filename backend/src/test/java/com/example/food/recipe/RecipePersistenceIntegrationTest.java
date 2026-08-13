package com.example.food.recipe;

import com.example.food.ai.recipe.dto.RecipeGenerateRequest;
import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.example.food.recipe.dto.RecipeHistoryDetailResponse;
import com.example.food.recipe.dto.RecipeHistorySummaryResponse;
import com.example.food.recipe.dto.SaveRecipeRequest;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecipePersistenceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SearchLogService searchLogService;

    @Autowired
    private SavedRecipeService savedRecipeService;

    @Test
    void persistsSearchAndSavedRecipeThenReloadsHistory() {
        jdbcTemplate.update(
                "INSERT INTO users (phone, nickname, role, enabled) VALUES (?, ?, 'USER', 1)",
                "13900000106",
                "菜谱测试用户"
        );
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE phone = ?",
                Long.class,
                "13900000106"
        );
        AuthPrincipal principal = new AuthPrincipal(userId, "13900000106", AppRole.USER);

        RecipeGenerateRequest generateRequest = new RecipeGenerateRequest(
                "番茄、鸡蛋",
                "dinner",
                "balanced",
                "text"
        );
        RecipeGenerateResponse generatedRecipe = recipeResponse(null);
        Long searchLogId = searchLogService.record(
                generateRequest,
                generatedRecipe,
                principal,
                "anonymous-integration-test"
        );

        RecipeHistoryDetailResponse saved = savedRecipeService.save(
                saveRequest(searchLogId),
                principal,
                "anonymous-integration-test"
        );
        jdbcTemplate.update(
                "INSERT INTO users (phone, nickname, role, enabled) VALUES (?, ?, 'USER', 1)",
                "13900000107",
                "其他菜谱用户"
        );
        Long otherUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE phone = ?",
                Long.class,
                "13900000107"
        );
        jdbcTemplate.update(
                "INSERT INTO recipe_records (user_id, title) VALUES (?, ?)",
                otherUserId,
                "其他用户的菜谱"
        );
        Long otherRecipeId = jdbcTemplate.queryForObject(
                "SELECT id FROM recipe_records WHERE user_id = ? AND title = ?",
                Long.class,
                otherUserId,
                "其他用户的菜谱"
        );

        List<RecipeHistorySummaryResponse> history = savedRecipeService.list(userId, 20, 0);
        RecipeHistoryDetailResponse detail = savedRecipeService.detail(userId, saved.id());

        assertThat(searchLogId).isNotNull();
        assertThat(saved.id()).isNotNull();
        assertThat(history).extracting(RecipeHistorySummaryResponse::id).containsExactly(saved.id());
        assertThat(detail.recipe().title()).isEqualTo("番茄炒鸡蛋");
        assertThat(detail.recipe().ingredients()).hasSize(2);
        assertThat(detail.recipe().steps()).hasSize(2);
        assertThat(detail.recipe().missingIngredients()).singleElement().satisfies(item -> {
            assertThat(item.name()).isEqualTo("鸡蛋");
            assertThat(item.substitutes()).containsExactly("嫩豆腐");
        });
        assertThat(detail.recipe().explanation().pairingLogic()).isEqualTo("番茄的酸甜与鸡蛋的鲜香互补");
        assertThat(detail.searchIngredients()).isEqualTo("番茄、鸡蛋");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recipe_ingredients WHERE recipe_id = ?",
                Integer.class,
                saved.id()
        )).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recipe_steps WHERE recipe_id = ?",
                Integer.class,
                saved.id()
        )).isEqualTo(2);
        assertThatThrownBy(() -> savedRecipeService.detail(userId, otherRecipeId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode().value())
                        .isEqualTo(403));
    }

    private SaveRecipeRequest saveRequest(Long searchLogId) {
        RecipeGenerateResponse recipe = recipeResponse(searchLogId);
        return new SaveRecipeRequest(
                searchLogId,
                recipe.title(),
                recipe.summary(),
                recipe.effects(),
                recipe.ingredients(),
                recipe.missingIngredients(),
                recipe.steps(),
                recipe.tips(),
                recipe.videoKeywords(),
                recipe.explanation(),
                recipe.provider(),
                recipe.model()
        );
    }

    private RecipeGenerateResponse recipeResponse(Long searchLogId) {
        return new RecipeGenerateResponse(
                "番茄炒鸡蛋",
                "家常快手菜",
                List.of("补充蛋白质"),
                List.of(
                        new RecipeGenerateResponse.Ingredient("番茄", "2个"),
                        new RecipeGenerateResponse.Ingredient("鸡蛋", "3个")
                ),
                List.of(new RecipeGenerateResponse.MissingIngredient(
                        "鸡蛋",
                        "3个",
                        List.of("嫩豆腐"),
                        "用户未提供鸡蛋"
                )),
                List.of(
                        new RecipeGenerateResponse.Step(1, "准备食材", "番茄切块并打散鸡蛋", 5),
                        new RecipeGenerateResponse.Step(2, "翻炒", "依次炒熟鸡蛋和番茄", 8)
                ),
                List.of("番茄出汁后再调味"),
                List.of("番茄炒鸡蛋教程"),
                new RecipeGenerateResponse.Explanation(
                        "番茄的酸甜与鸡蛋的鲜香互补",
                        "包含蛋白质与维生素",
                        "分开炒制能保持嫩度"
                ),
                "qwen",
                "qwen-plus",
                searchLogId
        );
    }
}
