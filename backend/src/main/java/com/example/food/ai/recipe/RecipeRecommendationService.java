package com.example.food.ai.recipe;

import com.example.food.ai.qwen.QwenRecipeClient;
import com.example.food.ai.recipe.dto.RecipeGenerateRequest;
import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.example.food.recipe.SearchLogService;
import com.example.food.security.AuthPrincipal;
import org.springframework.stereotype.Service;

@Service
public class RecipeRecommendationService {

    private final QwenRecipeClient qwenRecipeClient;
    private final SearchLogService searchLogService;

    public RecipeRecommendationService(QwenRecipeClient qwenRecipeClient, SearchLogService searchLogService) {
        this.qwenRecipeClient = qwenRecipeClient;
        this.searchLogService = searchLogService;
    }

    public RecipeGenerateResponse generate(RecipeGenerateRequest request) {
        return generate(request, null, null);
    }

    public RecipeGenerateResponse generate(
            RecipeGenerateRequest request,
            AuthPrincipal principal,
            String anonymousId
    ) {
        RecipeGenerateResponse response = qwenRecipeClient.generateRecipe(buildPrompt(request));
        Long searchLogId = searchLogService.record(request, response, principal, anonymousId);
        return response.withSearchLogId(searchLogId);
    }

    private String buildPrompt(RecipeGenerateRequest request) {
        return """
                请根据以下信息生成一份适合家庭烹饪的中文菜谱。

                用户已有食材：%s
                餐次：%s
                饮食目标：%s
                输入方式：%s
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
                  }
                }

                ingredients 必须列出完成菜谱所需的全部食材。
                将“用户已有食材”视为用户已经提供的食材，并按常见别名和语义判断是否已有。
                仅将用户没有提供、但菜谱需要的食材放入 missingIngredients；没有缺失食材时返回空数组。
                只为 missingIngredients 中的缺失食材提供 substitutes，不要为已有食材提供替代建议。
                effects 与 explanation 只能提供一般饮食和烹饪信息，不得作出疾病治疗、预防或疗效保证等医疗承诺。
                """.formatted(
                safeText(request.ingredients()),
                safeText(request.mealType()),
                safeText(request.goal()),
                safeText(request.searchMode()),
                regenerationContext(request)
        );
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safeText(String value) {
        return hasText(value) ? value.trim() : "未指定";
    }
}
