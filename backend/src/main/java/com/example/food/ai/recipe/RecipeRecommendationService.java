package com.example.food.ai.recipe;

import com.example.food.ai.qwen.QwenRecipeClient;
import com.example.food.ai.recipe.dto.RecipeGenerateRequest;
import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.example.food.pantry.UserPantryService;
import com.example.food.recipe.SearchLogService;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.user.health.UserHealthProfileService;
import com.example.food.user.nutrition.UserNutritionTargetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;

@Service
public class RecipeRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecipeRecommendationService.class);

    private static final int MAX_PREFERENCE_TEXT_LENGTH = 80;
    private static final int MAX_PREFERENCE_ITEM_COUNT = 20;
    private static final int MAX_PREFERENCE_ITEM_LENGTH = 40;

    private final QwenRecipeClient qwenRecipeClient;
    private final SearchLogService searchLogService;
    private final UserPantryService userPantryService;
    private final UserHealthProfileService userHealthProfileService;
    private final UserNutritionTargetService userNutritionTargetService;
    private final com.example.food.recipe.RecommendationFeedbackService recommendationFeedbackService;

    @Autowired
    public RecipeRecommendationService(
            QwenRecipeClient qwenRecipeClient,
            SearchLogService searchLogService,
            UserPantryService userPantryService,
            UserHealthProfileService userHealthProfileService,
            com.example.food.recipe.RecommendationFeedbackService recommendationFeedbackService,
            UserNutritionTargetService userNutritionTargetService
    ) {
        this.qwenRecipeClient = qwenRecipeClient;
        this.searchLogService = searchLogService;
        this.userPantryService = userPantryService;
        this.userHealthProfileService = userHealthProfileService;
        this.recommendationFeedbackService = recommendationFeedbackService;
        this.userNutritionTargetService = userNutritionTargetService;
    }

    public RecipeRecommendationService(
            QwenRecipeClient qwenRecipeClient,
            SearchLogService searchLogService,
            UserPantryService userPantryService,
            UserHealthProfileService userHealthProfileService,
            com.example.food.recipe.RecommendationFeedbackService recommendationFeedbackService
    ) {
        this(
                qwenRecipeClient,
                searchLogService,
                userPantryService,
                userHealthProfileService,
                recommendationFeedbackService,
                null
        );
    }

    public RecipeRecommendationService(
            QwenRecipeClient qwenRecipeClient,
            SearchLogService searchLogService,
            UserPantryService userPantryService,
            UserHealthProfileService userHealthProfileService
    ) {
        this(qwenRecipeClient, searchLogService, userPantryService, userHealthProfileService, null);
    }

    public RecipeGenerateResponse generate(RecipeGenerateRequest request) {
        return generate(request, null, null);
    }

    public RecipeGenerateResponse generate(
            RecipeGenerateRequest request,
            AuthPrincipal principal,
            String anonymousId
    ) {
        RecipeGenerateResponse response = qwenRecipeClient.generateRecipe(promptFor(request, principal));
        return persist(request, response, principal, anonymousId);
    }

    public String promptFor(RecipeGenerateRequest request, AuthPrincipal principal) {
        UserHealthProfileService.RecommendationContext healthProfile = healthProfile(principal);
        UserNutritionTargetService.RecommendationContext nutritionTarget = nutritionTarget(principal);
        String feedbackContext = feedbackContext(principal);
        return buildPrompt(request, pantryIngredients(principal), healthProfile, nutritionTarget, feedbackContext);
    }

    public RecipeGenerateResponse persist(
            RecipeGenerateRequest request,
            RecipeGenerateResponse response,
            AuthPrincipal principal,
            String anonymousId
    ) {
        Long searchLogId = searchLogService.record(request, response, principal, anonymousId);
        return response.withSearchLogId(searchLogId);
    }

    private String buildPrompt(
            RecipeGenerateRequest request,
            List<String> pantryIngredients,
            UserHealthProfileService.RecommendationContext healthProfile,
            UserNutritionTargetService.RecommendationContext nutritionTarget,
            String feedbackContext
    ) {
        return """
                请根据以下信息生成一份适合家庭烹饪的中文菜谱。

                本次指定食材：%s
                用户库存食材：%s
                餐次：%s
                饮食目标：%s
                输入方式：%s
                %s
                %s

                必须只返回 JSON，不要返回 Markdown、代码块或额外说明。
                严格使用以下 JSON 字段：
                {
                  "title": "菜名",
                  "summary": "一句话简介",
                  "effects": ["饮食价值描述"],
                  "ingredients": [{"name": "全部所需食材名称", "amount": "用量"}],
                  "missingIngredients": [{
                    "name": "缺失食材名称",
                    "amount": "所需用量",
                    "substitutes": ["可替代食材"],
                    "reason": "判定为缺失以及替代建议的简短理由"
                  }],
                  "steps": [{"order": 1, "title": "步骤标题", "description": "步骤说明", "durationMinutes": 5}],
                  "tips": ["烹饪建议"],
                  "videoKeywords": ["适合搜索教学视频的关键词"],
                  "explanation": {
                    "pairingLogic": "食材搭配逻辑",
                    "nutrition": "客观、克制的营养说明",
                    "cookingPrinciple": "关键烹饪原理"
                  },
                  "nutritionEstimate": {
                    "servings": 2,
                    "caloriesKcal": 420,
                    "proteinG": 24,
                    "fatG": 16,
                    "carbohydrateG": 42,
                    "source": "AI_ESTIMATE"
                  }
                }

                ingredients 必须列出完成菜谱所需的全部食材。
                将“本次指定食材”和“用户库存食材”都视为用户可用的已有食材，并按常见别名和语义判断是否已有。
                仅将用户没有提供、但菜谱需要的食材放入 missingIngredients；没有缺失食材时返回空数组。
                只为 missingIngredients 中的缺失食材提供 substitutes，不要为已有食材提供替代建议。
                nutritionEstimate 必须是每份估算，servings 范围 1-20，caloriesKcal 范围 1-3000，proteinG 和 fatG 范围 0-300，carbohydrateG 范围 0-500，source 固定为 AI_ESTIMATE。若无法同时给出全部合法数值，则 nutritionEstimate 返回 null。所有营养内容必须标记为 AI 估算，仅供一般饮食参考，不得作出达标、超标或医疗判断。
                effects 与 explanation 只能提供一般饮食和烹饪信息，不得作出疾病治疗、预防或疗效保证等医疗承诺。
                """.formatted(
                safeText(request.ingredients()),
                safeIngredients(pantryIngredients),
                safeText(request.mealType()),
                safeText(resolveGoal(request)),
                safeText(request.searchMode()),
                requestContext(request, healthProfile, nutritionTarget),
                hasText(feedbackContext) ? feedbackContext : ""
        );
    }

    private String feedbackContext(AuthPrincipal principal) {
        if (recommendationFeedbackService == null
                || principal == null
                || principal.role() != AppRole.USER) {
            return "";
        }
        try {
            com.example.food.recipe.RecommendationFeedbackService.FeedbackContext context =
                    recommendationFeedbackService.context(principal.id());
            return context == null ? "" : context.promptSection();
        } catch (RuntimeException exception) {
            log.warn("读取推荐反馈失败，继续使用原推荐流程，userId={}", principal.id(), exception);
            return "";
        }
    }

    private List<String> pantryIngredients(AuthPrincipal principal) {
        if (principal == null || principal.role() != AppRole.USER) {
            return List.of();
        }
        return userPantryService.listIngredientNames(principal.id());
    }

    private UserHealthProfileService.RecommendationContext healthProfile(AuthPrincipal principal) {
        if (principal == null || principal.role() != AppRole.USER) {
            return null;
        }
        return userHealthProfileService.getRecommendationContext(principal.id());
    }

    private UserNutritionTargetService.RecommendationContext nutritionTarget(AuthPrincipal principal) {
        if (userNutritionTargetService == null
                || principal == null
                || principal.role() != AppRole.USER) {
            return null;
        }
        try {
            return userNutritionTargetService.getRecommendationContext(principal.id());
        } catch (RuntimeException exception) {
            log.warn("读取每日营养目标失败，继续使用原推荐流程，userId={}", principal.id(), exception);
            return null;
        }
    }

    private String resolveGoal(RecipeGenerateRequest request) {
        if (hasText(request.goal())) {
            return request.goal();
        }
        if (request.dietPreference() == null) {
            return null;
        }
        return normalizeText(request.dietPreference().defaultGoal(), MAX_PREFERENCE_TEXT_LENGTH);
    }

    private String requestContext(
            RecipeGenerateRequest request,
            UserHealthProfileService.RecommendationContext healthProfile,
            UserNutritionTargetService.RecommendationContext nutritionTarget
    ) {
        String regenerationContext = regenerationContext(request);
        String dietPreferenceContext = dietPreferenceContext(request.dietPreference());
        String context = regenerationContext;
        if (hasText(dietPreferenceContext)) {
            context += "\n" + dietPreferenceContext;
        }
        String healthProfileContext = healthProfileContext(healthProfile);
        if (hasText(healthProfileContext)) {
            context += "\n" + healthProfileContext;
        }
        String nutritionTargetContext = nutritionTargetContext(nutritionTarget);
        if (hasText(nutritionTargetContext)) {
            context += "\n" + nutritionTargetContext;
        }
        return context;
    }

    private String regenerationContext(RecipeGenerateRequest request) {
        if (!hasText(request.regenerationPreference()) && !hasText(request.previousTitle())) {
            return "生成类型：首次生成";
        }
        return """
                这是一次菜谱再生成。
                上一版菜名：%s
                调整方向：%s
                请根据调整方向生成有明显变化的新版本。
                """.formatted(
                safeText(request.previousTitle()),
                safeText(request.regenerationPreference())
        ).strip();
    }

    private String dietPreferenceContext(RecipeGenerateRequest.DietPreference dietPreference) {
        if (dietPreference == null) {
            return "";
        }
        String taste = normalizeText(dietPreference.taste(), MAX_PREFERENCE_TEXT_LENGTH);
        String defaultGoal = normalizeText(dietPreference.defaultGoal(), MAX_PREFERENCE_TEXT_LENGTH);
        List<String> avoidIngredients = normalizeIngredients(dietPreference.avoidIngredients());
        List<String> allergenIngredients = normalizeIngredients(dietPreference.allergenIngredients());
        if (!hasText(taste) && !hasText(defaultGoal)
                && avoidIngredients.isEmpty() && allergenIngredients.isEmpty()) {
            return "";
        }
        return """
                用户饮食偏好与安全约束：
                口味偏好：%s
                忌口食材（不可使用）：%s
                过敏食材（不可使用）：%s
                忌口食材和过敏食材均不可使用，也不可作为替代食材推荐。
                """.formatted(
                safeText(taste),
                safeIngredients(avoidIngredients),
                safeIngredients(allergenIngredients)
        ).strip();
    }

    private String healthProfileContext(UserHealthProfileService.RecommendationContext healthProfile) {
        if (healthProfile == null) {
            return "";
        }
        return """
                用户健康档案（仅用于一般健康饮食推荐，不得作为医疗诊断或治疗依据）：
                年龄段：%s
                身高：%s 厘米
                体重：%s 千克
                身体质量指数（BMI，仅作一般参考）：%s
                日常活动量：%s
                请结合上述信息和饮食目标调整食材搭配、烹饪方式与分量建议；不得输出疾病诊断、治疗方案、处方、化验指标解读、绝对热量承诺或疗效保证。
                """.formatted(
                ageRangeLabel(healthProfile.ageRange()),
                formatNumber(healthProfile.heightCm()),
                formatNumber(healthProfile.weightKg()),
                formatNumber(healthProfile.bmi()),
                activityLevelLabel(healthProfile.activityLevel())
        ).strip();
    }

    private String nutritionTargetContext(UserNutritionTargetService.RecommendationContext nutritionTarget) {
        if (nutritionTarget == null) {
            return "";
        }
        return """
                用户设定的每日营养目标（软偏好，仅用于调整菜谱搭配与分量）：
                热量：%s 千卡
                蛋白质：%s 克
                脂肪：%s 克
                碳水：%s 克
                请在不违反本次输入、忌口和过敏等更高优先级约束的前提下参考这些目标；营养数值仍是 AI 估算，仅供一般饮食参考，不保证精确达到目标，也不是诊断或治疗建议。
                """.formatted(
                formatNumber(nutritionTarget.caloriesKcal()),
                formatNumber(nutritionTarget.proteinG()),
                formatNumber(nutritionTarget.fatG()),
                formatNumber(nutritionTarget.carbohydrateG())
        ).strip();
    }

    private String ageRangeLabel(String ageRange) {
        return switch (ageRange) {
            case "AGE_18_29" -> "18-29 岁";
            case "AGE_30_44" -> "30-44 岁";
            case "AGE_45_59" -> "45-59 岁";
            case "AGE_60_PLUS" -> "60 岁及以上";
            default -> "未指定";
        };
    }

    private String activityLevelLabel(String activityLevel) {
        return switch (activityLevel) {
            case "LOW" -> "低";
            case "MODERATE" -> "中等";
            case "HIGH" -> "高";
            default -> "未指定";
        };
    }

    private String formatNumber(java.math.BigDecimal value) {
        return value == null ? "未指定" : value.stripTrailingZeros().toPlainString();
    }

    private List<String> normalizeIngredients(List<String> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String ingredient : ingredients) {
            String item = normalizeText(ingredient, MAX_PREFERENCE_ITEM_LENGTH);
            if (hasText(item)) {
                normalized.add(item);
            }
            if (normalized.size() == MAX_PREFERENCE_ITEM_COUNT) {
                break;
            }
        }
        return List.copyOf(normalized);
    }

    private String normalizeText(String value, int maxLength) {
        if (!hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String safeIngredients(List<String> ingredients) {
        return ingredients.isEmpty() ? "未指定" : String.join("、", ingredients);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safeText(String value) {
        return hasText(value) ? value.trim() : "未指定";
    }
}
