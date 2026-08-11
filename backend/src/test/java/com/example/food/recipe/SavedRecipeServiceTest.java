package com.example.food.recipe;

import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.example.food.recipe.dto.RecipeHistoryDetailResponse;
import com.example.food.recipe.dto.SaveRecipeRequest;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavedRecipeServiceTest {

    @Mock
    private SearchLogMapper searchLogMapper;

    @Mock
    private RecipeRecordMapper recipeRecordMapper;

    @Mock
    private RecipeIngredientMapper recipeIngredientMapper;

    @Mock
    private RecipeStepMapper recipeStepMapper;

    private SavedRecipeService savedRecipeService;

    @BeforeEach
    void setUp() {
        savedRecipeService = new SavedRecipeService(
                searchLogMapper,
                recipeRecordMapper,
                recipeIngredientMapper,
                recipeStepMapper,
                new ObjectMapper()
        );
    }

    private void stubRecipeInsert() {
        doAnswer(invocation -> {
            RecipeRecord record = invocation.getArgument(0);
            record.setId(99L);
            return 1;
        }).when(recipeRecordMapper).insert(any(RecipeRecord.class));
    }

    @Test
    void savesRecipeWithAuthenticatedUserIdAndChildren() {
        stubRecipeInsert();
        SearchLog searchLog = searchLog(10L, 7L, null);
        when(searchLogMapper.selectById(10L)).thenReturn(searchLog);

        RecipeHistoryDetailResponse result = savedRecipeService.save(
                request(),
                new AuthPrincipal(7L, "13800138000", AppRole.USER),
                null
        );

        ArgumentCaptor<RecipeRecord> recordCaptor = ArgumentCaptor.forClass(RecipeRecord.class);
        ArgumentCaptor<RecipeIngredient> ingredientCaptor = ArgumentCaptor.forClass(RecipeIngredient.class);
        ArgumentCaptor<RecipeStep> stepCaptor = ArgumentCaptor.forClass(RecipeStep.class);
        org.mockito.Mockito.verify(recipeRecordMapper).insert(recordCaptor.capture());
        org.mockito.Mockito.verify(recipeIngredientMapper).insert(ingredientCaptor.capture());
        org.mockito.Mockito.verify(recipeStepMapper).insert(stepCaptor.capture());

        assertThat(recordCaptor.getValue().getUserId()).isEqualTo(7L);
        assertThat(recordCaptor.getValue().getSearchLogId()).isEqualTo(10L);
        assertThat(recordCaptor.getValue().getTitle()).isEqualTo("番茄炒蛋");
        assertThat(ingredientCaptor.getValue().getRecipeId()).isEqualTo(99L);
        assertThat(stepCaptor.getValue().getRecipeId()).isEqualTo(99L);
        assertThat(result.id()).isEqualTo(99L);
    }

    @Test
    void rejectsSearchLogOwnedByAnotherUser() {
        when(searchLogMapper.selectById(10L)).thenReturn(searchLog(10L, 8L, null));

        assertThatThrownBy(() -> savedRecipeService.save(
                request(),
                new AuthPrincipal(7L, "13800138000", AppRole.USER),
                null
        )).hasMessageContaining("无权使用该搜索记录");
    }

    @Test
    void acceptsAnonymousSearchLogWhenAnonymousIdMatches() {
        stubRecipeInsert();
        when(searchLogMapper.selectById(10L)).thenReturn(searchLog(10L, null, "anonymous-12345678"));

        RecipeHistoryDetailResponse result = savedRecipeService.save(
                request(),
                new AuthPrincipal(7L, "13800138000", AppRole.USER),
                "anonymous-12345678"
        );

        assertThat(result.id()).isEqualTo(99L);
    }

    private SearchLog searchLog(Long id, Long userId, String anonymousId) {
        SearchLog searchLog = new SearchLog();
        searchLog.setId(id);
        searchLog.setUserId(userId);
        searchLog.setAnonymousId(anonymousId);
        searchLog.setQueryText("番茄、鸡蛋");
        searchLog.setMealType("dinner");
        searchLog.setGoal("balanced");
        return searchLog;
    }

    private SaveRecipeRequest request() {
        return new SaveRecipeRequest(
                10L,
                "番茄炒蛋",
                "家常快手菜",
                List.of("补充蛋白质"),
                List.of(new RecipeGenerateResponse.Ingredient("番茄", "2个")),
                List.of(new RecipeGenerateResponse.Step(1, "备菜", "番茄切块", 5)),
                List.of("先炒鸡蛋"),
                List.of("番茄炒蛋教程"),
                "qwen",
                "qwen-plus"
        );
    }
}
