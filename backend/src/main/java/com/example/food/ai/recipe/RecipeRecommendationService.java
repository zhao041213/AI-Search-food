package com.example.food.ai.recipe;

import com.example.food.ai.qwen.QwenRecipeClient;
import com.example.food.ai.recipe.dto.RecipeGenerateRequest;
import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import org.springframework.stereotype.Service;

@Service
public class RecipeRecommendationService {

    private final QwenRecipeClient qwenRecipeClient;

    public RecipeRecommendationService(QwenRecipeClient qwenRecipeClient) {
        this.qwenRecipeClient = qwenRecipeClient;
    }

    public RecipeGenerateResponse generate(RecipeGenerateRequest request) {
        return qwenRecipeClient.generateRecipe(buildPrompt(request));
    }

    private String buildPrompt(RecipeGenerateRequest request) {
        return """
                请根据以下用户输入生成一份适合家庭烹饪的中文菜谱。

                用户食材：%s
                餐次：%s
                饮食目标：%s
                输入方式：%s

                必须只返回 JSON，不要 Markdown，不要代码块，不要解释。
                JSON 字段：
                {
                  "title": "菜名",
                  "summary": "一句话简介",
                  "effects": ["功效1", "功效2"],
                  "ingredients": [{"name": "食材名", "amount": "用量"}],
                  "steps": [{"order": 1, "title": "步骤标题", "description": "步骤说明", "durationMinutes": 5}],
                  "tips": ["烹饪建议"],
                  "videoKeywords": ["适合搜索教学视频的关键词"]
                }
                """.formatted(
                safeText(request.ingredients()),
                safeText(request.mealType()),
                safeText(request.goal()),
                safeText(request.searchMode())
        );
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "未指定" : value.trim();
    }
}
