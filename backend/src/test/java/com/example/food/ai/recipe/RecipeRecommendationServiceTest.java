package com.example.food.ai.recipe;

import com.example.food.ai.qwen.QwenRecipeClient;
import com.example.food.ai.recipe.dto.RecipeGenerateRequest;
import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.example.food.pantry.UserPantryService;
import com.example.food.recipe.SearchLogService;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.user.health.UserHealthProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeRecommendationServiceTest {

    @Mock
    private QwenRecipeClient qwenRecipeClient;

    @Mock
    private SearchLogService searchLogService;

    @Mock
    private UserPantryService userPantryService;

    @Mock
    private UserHealthProfileService userHealthProfileService;

    @InjectMocks
    private RecipeRecommendationService recipeRecommendationService;

    @Test
    void returnsSearchLogIdAfterSuccessfulGeneration() {
        RecipeGenerateRequest request = new RecipeGenerateRequest("番茄", "dinner", "balanced", "text");
        RecipeGenerateResponse generated = recipeResponse();
        when(qwenRecipeClient.generateRecipe(anyString())).thenReturn(generated);
        when(searchLogService.record(request, generated, null, "anonymous-12345678")).thenReturn(88L);

        RecipeGenerateResponse result = recipeRecommendationService.generate(
                request,
                null,
                "anonymous-12345678"
        );

        assertThat(result.searchLogId()).isEqualTo(88L);
        verify(searchLogService).record(request, generated, null, "anonymous-12345678");
    }

    @Test
    void legacyRequestConstructorLeavesRegenerationFieldsEmpty() {
        RecipeGenerateRequest request = new RecipeGenerateRequest("番茄", "dinner", "balanced", "text");

        assertThat(request.regenerationPreference()).isNull();
        assertThat(request.previousTitle()).isNull();
        assertThat(request.dietPreference()).isNull();
    }

    @Test
    void generationPromptUsesNormalizedDietPreferenceAndDefaultGoal() {
        RecipeGenerateRequest request = new RecipeGenerateRequest(
                "番茄",
                "dinner",
                "  ",
                "text",
                null,
                null,
                new RecipeGenerateRequest.DietPreference(
                        "  清淡  ",
                        "  增肌  ",
                        List.of(" 香菜 ", "香菜", "  葱  ", " "),
                        List.of(" 花生 ", "花生", " 牛奶 ")
                )
        );
        RecipeGenerateResponse generated = recipeResponse();
        when(qwenRecipeClient.generateRecipe(anyString())).thenReturn(generated);

        recipeRecommendationService.generate(request);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(qwenRecipeClient).generateRecipe(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("饮食目标：增肌")
                .contains("口味偏好：清淡")
                .contains("忌口食材（不可使用）：香菜、葱")
                .contains("过敏食材（不可使用）：花生、牛奶")
                .contains("忌口食材和过敏食材均不可使用")
                .doesNotContain("香菜、香菜")
                .doesNotContain("花生、花生");
    }

    @Test
    void requestGoalTakesPriorityOverDietPreferenceDefaultGoal() {
        RecipeGenerateRequest request = new RecipeGenerateRequest(
                "番茄",
                "dinner",
                "  减脂  ",
                "text",
                null,
                null,
                new RecipeGenerateRequest.DietPreference("清淡", "增肌", null, null)
        );
        when(qwenRecipeClient.generateRecipe(anyString())).thenReturn(recipeResponse());

        recipeRecommendationService.generate(request);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(qwenRecipeClient).generateRecipe(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("饮食目标：减脂")
                .doesNotContain("饮食目标：增肌");
    }

    @Test
    void generationPromptIncludesLoggedInUsersPantryIngredients() {
        RecipeGenerateRequest request = new RecipeGenerateRequest("番茄", "dinner", "balanced", "text");
        AuthPrincipal principal = new AuthPrincipal(7L, "13800138000", AppRole.USER);
        when(userPantryService.listIngredientNames(7L)).thenReturn(List.of("鸡蛋", "土豆"));
        when(qwenRecipeClient.generateRecipe(anyString())).thenReturn(recipeResponse());

        recipeRecommendationService.generate(request, principal, null);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(qwenRecipeClient).generateRecipe(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("本次指定食材：番茄")
                .contains("用户库存食材：鸡蛋、土豆")
                .contains("将“本次指定食材”和“用户库存食材”都视为用户可用的已有食材");
        verify(userPantryService).listIngredientNames(7L);
    }

    @Test
    void generationPromptIncludesLoggedInUsersHealthProfileWithoutMedicalClaims() {
        RecipeGenerateRequest request = new RecipeGenerateRequest("番茄", "dinner", "fat_loss", "text");
        AuthPrincipal principal = new AuthPrincipal(7L, "13800138000", AppRole.USER);
        when(userHealthProfileService.getRecommendationContext(7L)).thenReturn(
                new UserHealthProfileService.RecommendationContext(
                        "AGE_30_44",
                        new BigDecimal("172.0"),
                        new BigDecimal("64.0"),
                        "MODERATE",
                        new BigDecimal("21.6")
                )
        );
        when(qwenRecipeClient.generateRecipe(anyString())).thenReturn(recipeResponse());

        recipeRecommendationService.generate(request, principal, null);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(qwenRecipeClient).generateRecipe(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("用户健康档案")
                .contains("年龄段：30-44 岁")
                .contains("身高：172 厘米")
                .contains("体重：64 千克")
                .contains("BMI，仅作一般参考）：21.6")
                .contains("日常活动量：中等")
                .contains("不得输出疾病诊断、治疗方案、处方")
                .doesNotContain("化验指标解读：");
        verify(userHealthProfileService).getRecommendationContext(7L);
    }

    @Test
    void dietPreferenceCollectionsAreLengthLimitedBeforeAddingToPrompt() {
        List<String> avoidIngredients = new ArrayList<>();
        for (int index = 1; index <= 21; index++) {
            avoidIngredients.add("忌口" + index);
        }
        avoidIngredients.set(0, "超".repeat(45));
        RecipeGenerateRequest request = new RecipeGenerateRequest(
                "番茄",
                "dinner",
                null,
                "text",
                null,
                null,
                new RecipeGenerateRequest.DietPreference(
                        "甜".repeat(85),
                        null,
                        avoidIngredients,
                        null
                )
        );
        when(qwenRecipeClient.generateRecipe(anyString())).thenReturn(recipeResponse());

        recipeRecommendationService.generate(request);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(qwenRecipeClient).generateRecipe(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("口味偏好：" + "甜".repeat(80))
                .doesNotContain("甜".repeat(81))
                .contains("忌口食材（不可使用）：" + "超".repeat(40))
                .doesNotContain("超".repeat(41))
                .contains("忌口20")
                .doesNotContain("忌口21");
    }

    @Test
    void regenerationPromptDescribesNewContractAndStillRecordsSearchLog() {
        RecipeGenerateRequest request = new RecipeGenerateRequest(
                "番茄",
                "dinner",
                "balanced",
                "text",
                "减少用油并缩短烹饪时间",
                "番茄炒蛋",
                new RecipeGenerateRequest.DietPreference(
                        "酸辣",
                        "均衡",
                        List.of("香菜"),
                        List.of("花生")
                )
        );
        RecipeGenerateResponse generated = recipeResponse();
        when(qwenRecipeClient.generateRecipe(anyString())).thenReturn(generated);
        when(searchLogService.record(request, generated, null, "anonymous-12345678")).thenReturn(99L);

        RecipeGenerateResponse result = recipeRecommendationService.generate(
                request,
                null,
                "anonymous-12345678"
        );

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(qwenRecipeClient).generateRecipe(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt)
                .contains("本次指定食材：番茄")
                .contains("用户库存食材：未指定")
                .contains("上一版菜名：番茄炒蛋")
                .contains("调整方向：减少用油并缩短烹饪时间")
                .contains("口味偏好：酸辣")
                .contains("忌口食材（不可使用）：香菜")
                .contains("过敏食材（不可使用）：花生")
                .contains("忌口食材和过敏食材均不可使用")
                .contains("\"missingIngredients\"")
                .contains("\"explanation\"")
                .contains("仅将用户没有提供、但菜谱需要的食材放入 missingIngredients")
                .contains("只为 missingIngredients 中的缺失食材提供 substitutes")
                .contains("不得作出疾病治疗、预防或疗效保证等医疗承诺");
        assertThat(result.searchLogId()).isEqualTo(99L);
        verify(searchLogService).record(request, generated, null, "anonymous-12345678");
    }

    @Test
    void responseNormalizesNullableNewFields() {
        RecipeGenerateResponse response = new RecipeGenerateResponse(
                "番茄炒蛋",
                "家常快手菜",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "qwen",
                "qwen-plus"
        );

        assertThat(response.effects()).isEmpty();
        assertThat(response.ingredients()).isEmpty();
        assertThat(response.missingIngredients()).isEmpty();
        assertThat(response.steps()).isEmpty();
        assertThat(response.tips()).isEmpty();
        assertThat(response.videoKeywords()).isEmpty();
        assertThat(response.explanation()).isNotNull();
        assertThat(response.explanation().pairingLogic()).isEmpty();
        assertThat(response.explanation().nutrition()).isEmpty();
        assertThat(response.explanation().cookingPrinciple()).isEmpty();

        RecipeGenerateResponse.MissingIngredient missingIngredient =
                new RecipeGenerateResponse.MissingIngredient("鸡蛋", "2个", null, null);
        assertThat(missingIngredient.substitutes()).isEmpty();
        assertThat(missingIngredient.reason()).isEmpty();
    }

    @Test
    void doesNotRecordWhenAiGenerationThrows() {
        RecipeGenerateRequest request = new RecipeGenerateRequest("番茄", "dinner", "balanced", "text");
        when(qwenRecipeClient.generateRecipe(anyString())).thenThrow(new IllegalStateException("AI unavailable"));

        assertThatThrownBy(() -> recipeRecommendationService.generate(request, null, "anonymous-12345678"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AI unavailable");
        verifyNoInteractions(searchLogService);
    }

    private RecipeGenerateResponse recipeResponse() {
        return new RecipeGenerateResponse(
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
