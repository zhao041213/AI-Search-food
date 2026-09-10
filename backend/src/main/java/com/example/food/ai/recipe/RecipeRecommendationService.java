package com.example.food.ai.recipe;

import com.example.food.ai.qwen.QwenRecipeClient;
import com.example.food.ai.recipe.dto.RecipeGenerateRequest;
import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.example.food.pantry.UserPantryService;
import com.example.food.pantry.dto.PantryItemResponse;
import com.example.food.recipe.SearchLogService;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.user.health.UserHealthProfileService;
import com.example.food.user.healthnutrition.HealthNutritionService;
import com.example.food.user.healthnutrition.dto.NutritionValues;
import com.example.food.user.nutrition.UserNutritionTargetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class RecipeRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecipeRecommendationService.class);

    private static final int MAX_PREFERENCE_TEXT_LENGTH = 80;
    private static final int MAX_PREFERENCE_ITEM_COUNT = 20;
    private static final int MAX_PREFERENCE_ITEM_LENGTH = 40;
    private static final Map<String, String> INGREDIENT_ALIASES = Map.of(
            "西红柿", "番茄",
            "马铃薯", "土豆",
            "洋芋", "土豆",
            "青椒", "青辣椒"
    );

    private final QwenRecipeClient qwenRecipeClient;
    private final SearchLogService searchLogService;
    private final UserPantryService userPantryService;
    private final UserHealthProfileService userHealthProfileService;
    private final UserNutritionTargetService userNutritionTargetService;
    private final HealthNutritionService healthNutritionService;
    private final com.example.food.recipe.RecommendationFeedbackService recommendationFeedbackService;

    @Autowired
    public RecipeRecommendationService(
            QwenRecipeClient qwenRecipeClient,
            SearchLogService searchLogService,
            UserPantryService userPantryService,
            UserHealthProfileService userHealthProfileService,
            com.example.food.recipe.RecommendationFeedbackService recommendationFeedbackService,
            UserNutritionTargetService userNutritionTargetService,
            HealthNutritionService healthNutritionService
    ) {
        this.qwenRecipeClient = qwenRecipeClient;
        this.searchLogService = searchLogService;
        this.userPantryService = userPantryService;
        this.userHealthProfileService = userHealthProfileService;
        this.recommendationFeedbackService = recommendationFeedbackService;
        this.userNutritionTargetService = userNutritionTargetService;
        this.healthNutritionService = healthNutritionService;
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
                null,
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
        PreparedPrompt prepared = preparePrompt(request, principal);
        RecipeGenerateResponse response = qwenRecipeClient.generateRecipe(prepared.prompt())
                .withContextFlags(prepared.pantryReferenced(), prepared.pantryFallback(), prepared.healthNutritionReferenced());
        validateIngredientAlignment(request, response);
        return persist(request, response, principal, anonymousId);
    }

    public String promptFor(RecipeGenerateRequest request, AuthPrincipal principal) {
        return preparePrompt(request, principal).prompt();
    }

    public String recommendationBatchMode(RecipeGenerateRequest request) {
        return splitIngredientNames(request == null ? null : request.ingredients()).size() > 1
                ? "MEAL_COMBO"
                : "STYLE_VARIANTS";
    }

    public String batchRecipePrompt(
            String basePrompt,
            RecipeGenerateRequest request,
            int recipeIndex,
            int total
    ) {
        String mode = recommendationBatchMode(request);
        String variant = mode.equals("MEAL_COMBO")
                ? switch (recipeIndex) {
                    case 0 -> "以第一个核心食材为主角，设计一道适合搭配主食的主菜";
                    case 1 -> "以另一个核心食材为主角，设计一道清爽的配菜或汤菜";
                    default -> "综合剩余食材设计一道口味和烹饪方式不同的配菜";
                }
                : switch (recipeIndex) {
                    case 0 -> "家常下饭风格，步骤清晰、适合日常家庭烹饪";
                    case 1 -> "清爽低油风格，突出食材原味和营养搭配";
                    default -> "快速省时风格，适合工作日快速完成";
                };
        return basePrompt + """


                【多菜谱组合生成规则】
                本次请求必须生成第 %d 道，共 %d 道相互独立的菜谱，本道菜的定位是：%s。
                多种输入食材不要求全部放进同一道菜，应该拆分到三道可以搭配成一餐的独立菜品中。
                菜名只描述本道菜实际使用的核心食材，不要把所有输入食材拼接成一个超长菜名。
                本道菜至少使用一种用户指定食材；ingredients 和 steps 必须与本道菜名及实际做法一致。
                三道菜的风格、烹饪方式或搭配角色必须有明显区别。以上规则优先于前文要求将所有输入食材放入单道菜谱的描述。
                """.formatted(recipeIndex + 1, total, variant);
    }

    public PreparedPrompt preparePrompt(RecipeGenerateRequest request, AuthPrincipal principal) {
        PantrySelection pantry = pantrySelection(request, principal);
        HealthNutritionService.RecommendationContext unifiedHealthNutrition = unifiedHealthNutrition(request, principal);
        UserHealthProfileService.RecommendationContext healthProfile = unifiedHealthNutrition == null && request.includeHealthNutrition()
                ? healthProfile(principal) : null;
        UserNutritionTargetService.RecommendationContext nutritionTarget = unifiedHealthNutrition == null && request.includeHealthNutrition()
                ? nutritionTarget(principal) : null;
        String feedbackContext = feedbackContext(principal);
        return new PreparedPrompt(
                buildPrompt(request, pantry.ingredients(), pantry.details(), unifiedHealthNutrition, healthProfile, nutritionTarget, feedbackContext),
                pantry.referenced(),
                pantry.fallback(),
                request.includeHealthNutrition() && (unifiedHealthNutrition != null || healthProfile != null || nutritionTarget != null)
        );
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
            String pantryDetails,
            HealthNutritionService.RecommendationContext unifiedHealthNutrition,
            UserHealthProfileService.RecommendationContext healthProfile,
            UserNutritionTargetService.RecommendationContext nutritionTarget,
            String feedbackContext
    ) {
        return """
                请根据以下信息生成一份适合家庭烹饪的中文菜谱。

                本次指定食材：%s
                用户库存食材：%s
                用户库存明细：%s
                餐次：%s
                饮食目标：%s
                输入方式：%s
                %s
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
                如果本次标记为 AI 自主推荐食材，必须忽略历史输入食材，只根据餐次、目标、饮食边界和已启用的用户库存自主选择食材。
                将“本次指定食材”和“用户库存食材”都视为用户可用的已有食材，并按常见别名和语义判断是否已有。
                仅将用户没有提供、但菜谱需要的食材放入 missingIngredients；没有缺失食材时返回空数组。
                只为 missingIngredients 中的缺失食材提供 substitutes，不要为已有食材提供替代建议。
                nutritionEstimate 必须是每份估算，servings 范围 1-20，caloriesKcal 范围 1-3000，proteinG 和 fatG 范围 0-300，carbohydrateG 范围 0-500，source 固定为 AI_ESTIMATE。若无法同时给出全部合法数值，则 nutritionEstimate 返回 null。所有营养内容必须标记为 AI 估算，仅供一般饮食参考，不得作出达标、超标或医疗判断。
                effects 与 explanation 只能提供一般饮食和烹饪信息，不得作出疾病治疗、预防或疗效保证等医疗承诺。
                """.formatted(
                requestedIngredients(request),
                safeIngredients(pantryIngredients),
                safeText(pantryDetails),
                safeText(request.mealType()),
                safeText(resolveGoal(request)),
                safeText(request.searchMode()),
                ingredientUsageInstruction(request),
                requestContext(request, unifiedHealthNutrition, healthProfile, nutritionTarget),
                hasText(feedbackContext) ? feedbackContext : ""
        );
    }

    private String ingredientUsageInstruction(RecipeGenerateRequest request) {
        if (request.includeAiIngredientRecommendation()) {
            return "食材使用要求：本次由 AI 自主推荐食材，不得读取或复用历史输入食材。";
        }
        if (isRecognitionInput(request)) {
            return "食材使用要求：本次指定食材来自食材识别台，是用户刚刚确认的识别结果，必须严格围绕这些食材生成菜谱。每一种指定食材都必须出现在 ingredients 中并在 steps 中实际使用；不得忽略、替换为无关食材，或只把它放入 missingIngredients。可以补充必要辅料，但菜名、简介、步骤和食材清单必须与识别食材一致。";
        }
        return "食材使用要求：本次指定食材是用户明确提供的食材，必须作为菜谱的核心食材使用。每一种指定食材都必须出现在 ingredients 中并在 steps 中实际使用；不得忽略、替换为无关食材，或只把它放入 missingIngredients。可以补充必要辅料，但菜名、简介、步骤和食材清单必须与指定食材一致。";
    }

    private boolean isRecognitionInput(RecipeGenerateRequest request) {
        return "image".equalsIgnoreCase(request.searchMode())
                || "camera".equalsIgnoreCase(request.searchMode());
    }

    public void validateIngredientAlignment(RecipeGenerateRequest request, RecipeGenerateResponse response) {
        if (request == null || request.includeAiIngredientRecommendation() || !hasText(request.ingredients())) {
            return;
        }
        List<String> requested = splitIngredientNames(request.ingredients());
        Set<String> generated = response == null || response.ingredients() == null
                ? Set.of()
                : response.ingredients().stream()
                        .filter(item -> item != null && hasText(item.name()))
                        .map(RecipeGenerateResponse.Ingredient::name)
                        .map(this::normalizeIngredientName)
                        .collect(java.util.stream.Collectors.toSet());
        List<String> missing = requested.stream()
                .filter(item -> generated.stream().noneMatch(actual -> ingredientMatches(item, actual)))
                .toList();
        if (!missing.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "AI 返回的菜谱未围绕输入食材生成（缺少：" + String.join("、", missing) + "），请点击重试"
            );
        }
    }

    public void validateBatchIngredientAlignment(
            RecipeGenerateRequest request,
            List<RecipeGenerateResponse> responses
    ) {
        if (request == null || request.includeAiIngredientRecommendation() || !hasText(request.ingredients())) {
            return;
        }
        List<String> requested = splitIngredientNames(request.ingredients());
        Set<String> generated = responses == null ? Set.of() : responses.stream()
                .filter(java.util.Objects::nonNull)
                .flatMap(response -> response.ingredients().stream())
                .filter(item -> item != null && hasText(item.name()))
                .map(RecipeGenerateResponse.Ingredient::name)
                .map(this::normalizeIngredientName)
                .collect(java.util.stream.Collectors.toSet());
        List<String> missing = requested.stream()
                .filter(item -> generated.stream().noneMatch(actual -> ingredientMatches(item, actual)))
                .toList();
        if (!missing.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "AI 返回的菜谱组合未覆盖输入食材（缺少：" + String.join("、", missing) + "），请点击重试"
            );
        }
    }

    private List<String> splitIngredientNames(String ingredients) {
        if (!hasText(ingredients)) {
            return List.of();
        }
        return java.util.Arrays.stream(ingredients.split("[,，、；;\\n]+"))
                .map(String::trim)
                .filter(this::hasText)
                .map(this::normalizeIngredientName)
                .distinct()
                .toList();
    }

    private boolean ingredientMatches(String requested, String generated) {
        String requestedKey = normalizeIngredientName(requested);
        String generatedKey = normalizeIngredientName(generated);
        return requestedKey.equals(generatedKey)
                || requestedKey.contains(generatedKey)
                || generatedKey.contains(requestedKey);
    }

    private String normalizeIngredientName(String name) {
        String normalized = name == null ? "" : name.trim().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        return INGREDIENT_ALIASES.getOrDefault(normalized, normalized);
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

    private String requestedIngredients(RecipeGenerateRequest request) {
        return request.includeAiIngredientRecommendation()
                ? "AI 自主推荐食材（不得参考上一次输入的食材）"
                : safeText(request.ingredients());
    }

    private PantrySelection pantrySelection(RecipeGenerateRequest request, AuthPrincipal principal) {
        if (!request.includePantry() || principal == null || principal.role() != AppRole.USER) {
            return PantrySelection.empty();
        }
        try {
            List<PantryItemResponse> items = userPantryService.list(principal.id());
            List<String> names = items.stream()
                    .map(PantryItemResponse::ingredientName)
                    .filter(this::hasText)
                    .distinct()
                    .toList();
            String details = items.stream()
                    .filter(item -> hasText(item.ingredientName()))
                    .map(this::pantryItemLabel)
                    .collect(java.util.stream.Collectors.joining("；"));
            return new PantrySelection(names, details, true, false);
        } catch (RuntimeException exception) {
            log.warn("读取菜谱生成库存失败，降级为仅使用输入食材，userId={}", principal.id(), exception);
            return new PantrySelection(List.of(), "", false, true);
        }
    }

    private String pantryItemLabel(PantryItemResponse item) {
        String quantity = item.quantity() == null ? "数量未记录" : item.quantity().stripTrailingZeros().toPlainString();
        String unit = hasText(item.unit()) ? item.unit().trim() : "";
        String expiry = item.expireDate() == null ? "未设置到期日" : "到期 " + item.expireDate();
        return item.ingredientName().trim() + "（" + quantity + unit + "，" + expiry + "）";
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

    private HealthNutritionService.RecommendationContext unifiedHealthNutrition(RecipeGenerateRequest request, AuthPrincipal principal) {
        if (!request.includeHealthNutrition() || healthNutritionService == null
                || principal == null || principal.role() != AppRole.USER) {
            return null;
        }
        try {
            return healthNutritionService.recommendationContext(principal.id());
        } catch (RuntimeException exception) {
            log.warn("读取统一健康与营养设置失败，继续使用普通菜谱生成，userId={}", principal.id(), exception);
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
            HealthNutritionService.RecommendationContext unifiedHealthNutrition,
            UserHealthProfileService.RecommendationContext healthProfile,
            UserNutritionTargetService.RecommendationContext nutritionTarget
    ) {
        String regenerationContext = regenerationContext(request);
        String dietPreferenceContext = dietPreferenceContext(request.dietPreference());
        String context = regenerationContext;
        if (hasText(dietPreferenceContext)) {
            context += "\n" + dietPreferenceContext;
        }
        String healthProfileContext = unifiedHealthNutritionContext(unifiedHealthNutrition);
        if (!hasText(healthProfileContext)) {
            healthProfileContext = healthProfileContext(healthProfile);
        }
        if (hasText(healthProfileContext)) {
            context += "\n" + healthProfileContext;
        }
        String nutritionTargetContext = nutritionTargetContext(nutritionTarget);
        if (hasText(nutritionTargetContext)) {
            context += "\n" + nutritionTargetContext;
        }
        return context;
    }

    private String unifiedHealthNutritionContext(HealthNutritionService.RecommendationContext context) {
        if (context == null) return "";
        NutritionValues target = context.target();
        String targetText = target == null
                ? "当前未启用营养目标"
                : "每日热量=" + formatNumber(target.caloriesKcal()) + " 千卡，蛋白质=" + formatNumber(target.proteinG())
                + " 克，脂肪=" + formatNumber(target.fatG()) + " 克，碳水=" + formatNumber(target.carbohydrateG()) + " 克";
        return """
                用户健康与营养摘要（仅用于一般饮食参考，不得用于医疗诊断或治疗）：
                性别：%s，年龄：%s 岁，身高：%s 厘米，体重：%s 千克
                活动强度：%s，目标：%s
                饮食禁忌：%s
                过敏信息：%s
                特殊健康情况：%s
                当前采用营养目标：%s
                特殊人群或填写特殊健康情况时，只提供一般饮食参考，并提示咨询专业人士。
                """.formatted(
                safeText(context.gender()), context.age(), formatNumber(context.heightCm()), formatNumber(context.weightKg()),
                safeText(context.activityLevel()), safeText(context.goal()), safeIngredients(context.dietaryRestrictions()),
                safeIngredients(context.allergies()), safeText(context.specialHealthCondition()), targetText
        ).strip();
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

    public record PreparedPrompt(
            String prompt,
            boolean pantryReferenced,
            boolean pantryFallback,
            boolean healthNutritionReferenced
    ) {
    }

    private record PantrySelection(
            List<String> ingredients,
            String details,
            boolean referenced,
            boolean fallback
    ) {
        private static PantrySelection empty() {
            return new PantrySelection(List.of(), "", false, false);
        }
    }
}
