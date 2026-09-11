package com.example.food.ai.recipe;

import com.example.food.ai.qwen.QwenRecipeClient;
import com.example.food.ai.recipe.dto.RecipeGenerateRequest;
import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.example.food.pantry.UserPantryService;
import com.example.food.pantry.dto.PantryItemResponse;
import com.example.food.recipe.RecommendationFeedbackService;
import com.example.food.recipe.SearchLogService;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.user.health.UserHealthProfileService;
import com.example.food.user.healthnutrition.HealthNutritionService;
import com.example.food.user.nutrition.UserNutritionTargetService;
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
import static org.assertj.core.api.Assertions.assertThatCode;
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

    @Mock
    private UserNutritionTargetService userNutritionTargetService;

    @Mock
    private HealthNutritionService healthNutritionService;

    @Mock
    private RecommendationFeedbackService recommendationFeedbackService;

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
        assertThat(request.includePantry()).isFalse();
        assertThat(request.includeHealthNutrition()).isFalse();
    }

    @Test
    void aiIngredientRecommendationDoesNotReusePreviousIngredientInput() {
        RecipeGenerateRequest request = new RecipeGenerateRequest(
                null,
                "dinner",
                "balanced",
                "text",
                "换一份更有创意的做法",
                "上一道家常菜",
                null,
                false,
                false,
                true
        );
        when(qwenRecipeClient.generateRecipe(anyString())).thenReturn(recipeResponse());

        recipeRecommendationService.generate(request);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(qwenRecipeClient).generateRecipe(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("AI 自主推荐食材")
                .contains("不得参考上一次输入的食材")
                .doesNotContain("番茄、鸡蛋");
    }

    @Test
    void recognitionInputPromptRequiresEveryRecognizedIngredientToBeUsed() {
        RecipeGenerateRequest request = new RecipeGenerateRequest(
                "西红柿、鸡蛋",
                "dinner",
                "balanced",
                "image"
        );

        String prompt = recipeRecommendationService.promptFor(request, null);

        assertThat(prompt)
                .contains("输入方式：image")
                .contains("每一道菜严格只能使用本次输入食材中的一至两种")
                .contains("每道菜的核心搭配以后续组合规则为准");
    }

    @Test
    void rejectsRecipeThatDoesNotContainRequestedIngredients() {
        RecipeGenerateRequest request = new RecipeGenerateRequest("鸡肉", "dinner", "balanced", "image");
        when(qwenRecipeClient.generateRecipe(anyString())).thenReturn(recipeResponse());

        assertThatThrownBy(() -> recipeRecommendationService.generate(request))
                .hasMessageContaining("未围绕输入食材生成")
                .hasMessageContaining("鸡肉");
        verifyNoInteractions(searchLogService);
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
        RecipeGenerateRequest request = new RecipeGenerateRequest("番茄", "dinner", "balanced", "text", null, null, null, true, false);
        AuthPrincipal principal = new AuthPrincipal(7L, "13800138000", AppRole.USER);
        when(userPantryService.list(7L)).thenReturn(List.of(
                new PantryItemResponse(1L, "鸡蛋", "蛋奶", new BigDecimal("2"), "个", null, null),
                new PantryItemResponse(2L, "土豆", "蔬菜", new BigDecimal("3"), "个", null, null)
        ));
        when(qwenRecipeClient.generateRecipe(anyString())).thenReturn(recipeResponse());

        recipeRecommendationService.generate(request, principal, null);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(qwenRecipeClient).generateRecipe(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("本次指定食材：番茄")
                .contains("用户库存食材：鸡蛋、土豆")
                .contains("将“本次指定食材”和“用户库存食材”都视为用户可用的已有食材");
        verify(userPantryService).list(7L);
    }

    @Test
    void generationPromptIncludesLoggedInUsersHealthProfileWithoutMedicalClaims() {
        RecipeGenerateRequest request = new RecipeGenerateRequest("番茄", "dinner", "fat_loss", "text", null, null, null, false, true);
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
    void generationPromptIncludesEnabledNutritionTargetAsSoftPreference() {
        RecipeGenerateRequest request = new RecipeGenerateRequest("番茄", "dinner", "balanced", "text", null, null, null, false, true);
        AuthPrincipal principal = new AuthPrincipal(7L, "13800138000", AppRole.USER);
        when(userNutritionTargetService.getRecommendationContext(7L)).thenReturn(
                new UserNutritionTargetService.RecommendationContext(
                        new BigDecimal("2000"),
                        new BigDecimal("80"),
                        new BigDecimal("60"),
                        new BigDecimal("260")
                )
        );
        when(qwenRecipeClient.generateRecipe(anyString())).thenReturn(recipeResponse());

        recipeRecommendationService.generate(request, principal, null);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(qwenRecipeClient).generateRecipe(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("用户设定的每日营养目标（软偏好")
                .contains("热量：2000 千卡")
                .contains("蛋白质：80 克")
                .contains("不保证精确达到目标")
                .contains("本次输入、忌口和过敏等更高优先级约束");
        verify(userNutritionTargetService).getRecommendationContext(7L);
    }

    @Test
    void doesNotQueryPantryWhenSwitchIsOff() {
        RecipeGenerateRequest request = new RecipeGenerateRequest("番茄", "dinner", "balanced", "text");
        AuthPrincipal principal = new AuthPrincipal(7L, "13800138000", AppRole.USER);

        recipeRecommendationService.promptFor(request, principal);

        verifyNoInteractions(userPantryService);
    }

    @Test
    void generationPromptIncludesRecentRecommendationFeedback() {
        RecipeGenerateRequest request = new RecipeGenerateRequest("番茄", "dinner", "balanced", "text");
        AuthPrincipal principal = new AuthPrincipal(7L, "13800138000", AppRole.USER);
        when(recommendationFeedbackService.context(7L)).thenReturn(
                new RecommendationFeedbackService.FeedbackContext(
                        List.of("番茄炒蛋", "番茄"),
                        List.of("香菜拌豆腐", "香菜"),
                        List.of("土豆炖牛肉")
                )
        );
        when(qwenRecipeClient.generateRecipe(anyString())).thenReturn(recipeResponse());

        recipeRecommendationService.generate(request, principal, null);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(qwenRecipeClient).generateRecipe(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("用户近期推荐反馈")
                .contains("用户喜欢的菜名或食材（可优先考虑）：番茄炒蛋、番茄")
                .contains("用户不喜欢的菜名或食材（尽量避免重复，但本次明确输入优先）：香菜拌豆腐、香菜")
                .contains("用户近期已做过的菜名或食材（优先提供不同组合）：土豆炖牛肉");
    }

    @Test
    void feedbackReadFailureFallsBackToOriginalPrompt() {
        RecipeGenerateRequest request = new RecipeGenerateRequest("番茄", "dinner", "balanced", "text");
        AuthPrincipal principal = new AuthPrincipal(7L, "13800138000", AppRole.USER);
        when(recommendationFeedbackService.context(7L)).thenThrow(new IllegalStateException("feedback unavailable"));
        when(qwenRecipeClient.generateRecipe(anyString())).thenReturn(recipeResponse());

        recipeRecommendationService.generate(request, principal, null);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(qwenRecipeClient).generateRecipe(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).doesNotContain("用户近期推荐反馈");
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

    @Test
    void batchPromptPinsEachRecipeToTwoCoreIngredientsAndSearchableBilibiliKeywords() {
        RecipeGenerateRequest request = new RecipeGenerateRequest(
                "番茄、鸡蛋、牛肉",
                "dinner",
                "balanced",
                "text"
        );
        String basePrompt = recipeRecommendationService.promptFor(request, null);

        assertThat(recipeRecommendationService.recommendationBatchMode(request)).isEqualTo("MEAL_COMBO");
        assertThat(recipeRecommendationService.batchRecipePrompt(basePrompt, request, 0, 3))
                .contains("本道菜的两种核心食材固定为：番茄、鸡蛋")
                .contains("严格只能使用这一至两种核心食材")
                .contains("优先选择在 B 站容易找到教程")
                .contains("videoKeywords 必须提供");
        assertThat(recipeRecommendationService.batchRecipePrompt(basePrompt, request, 1, 3))
                .contains("本道菜的两种核心食材固定为：番茄、牛肉");
        assertThat(recipeRecommendationService.batchRecipePrompt(basePrompt, request, 2, 3))
                .contains("本道菜的两种核心食材固定为：鸡蛋、牛肉");
    }

    @Test
    void laterSingleIngredientPromptsIncludeEarlierRecipesToPreventRepetition() {
        RecipeGenerateRequest request = new RecipeGenerateRequest("螃蟹", "dinner", "balanced", "text");
        String prompt = recipeRecommendationService.batchRecipePrompt(
                recipeRecommendationService.promptFor(request, null),
                request,
                1,
                3,
                List.of(recipeResponseWithTitle("清蒸大闸蟹", "蒸制", "螃蟹"))
        );

        assertThat(prompt)
                .contains("清蒸大闸蟹")
                .contains("去重约束")
                .contains("不得复用前面菜谱的菜名")
                .contains("必须更换主要烹饪方式或成品形态");
    }

    @Test
    void identifiesRepeatedRecipeByTitleOrCompleteContent() {
        RecipeGenerateResponse earlier = recipeResponseWithTitle("清蒸大闸蟹", "蒸制", "螃蟹");

        assertThat(recipeRecommendationService.isDuplicateRecipe(
                recipeResponseWithTitle("清蒸大闸蟹", "蒸制", "螃蟹"),
                List.of(earlier)
        )).isTrue();
        assertThat(recipeRecommendationService.isDuplicateRecipe(
                recipeResponseWithTitle("姜葱炒螃蟹", "炒制", "螃蟹"),
                List.of(earlier)
        )).isFalse();
    }

    @Test
    void validatesEveryBatchRecipeContainsItsAssignedCorePair() {
        RecipeGenerateRequest request = new RecipeGenerateRequest(
                "番茄、鸡蛋、牛肉",
                "dinner",
                "balanced",
                "text"
        );

        assertThatCode(() -> recipeRecommendationService.validateBatchIngredientAlignment(request, List.of(
                recipeResponseWithIngredients("番茄", "鸡蛋"),
                recipeResponseWithIngredients("番茄", "牛肉"),
                recipeResponseWithIngredients("鸡蛋", "牛肉")
        ))).doesNotThrowAnyException();

        assertThatThrownBy(() -> recipeRecommendationService.validateBatchIngredientAlignment(request, List.of(
                recipeResponseWithIngredients("番茄", "鸡蛋"),
                recipeResponseWithIngredients("番茄"),
                recipeResponseWithIngredients("鸡蛋", "牛肉")
        )))
                .hasMessageContaining("第 2 道菜谱未同时使用指定的两种核心食材")
                .hasMessageContaining("牛肉");
    }

    @Test
    void rejectsBatchRecipeThatUsesMoreThanTwoRequestedIngredients() {
        RecipeGenerateRequest request = new RecipeGenerateRequest(
                "番茄、鸡蛋、牛肉",
                "dinner",
                "balanced",
                "text"
        );

        assertThatThrownBy(() -> recipeRecommendationService.validateBatchIngredientAlignment(request, List.of(
                recipeResponseWithIngredients("番茄", "鸡蛋", "牛肉"),
                recipeResponseWithIngredients("番茄", "牛肉"),
                recipeResponseWithIngredients("鸡蛋", "牛肉")
        )))
                .hasMessageContaining("第 1 道菜谱只能使用一至两种本次输入食材")
                .hasMessageContaining("牛肉");
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

    private RecipeGenerateResponse recipeResponseWithIngredients(String... names) {
        return recipeResponseWithTitle("家常菜", "烹饪", names);
    }

    private RecipeGenerateResponse recipeResponseWithTitle(String title, String stepTitle, String... names) {
        return new RecipeGenerateResponse(
                title,
                "家常做法",
                List.of(),
                java.util.Arrays.stream(names)
                        .map(name -> new RecipeGenerateResponse.Ingredient(name, "适量"))
                        .toList(),
                List.of(new RecipeGenerateResponse.Step(1, stepTitle, "完成烹饪", 10)),
                List.of(),
                List.of("家常做法"),
                "qwen",
                "qwen-plus"
        );
    }
}
