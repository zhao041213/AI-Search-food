package com.example.food.service;

import com.example.food.model.Recipe;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
public class RecipeService {

    private final List<Recipe> recipes = List.of(
            new Recipe(
                    "番茄炒蛋",
                    List.of("番茄", "鸡蛋", "葱", "盐", "糖"),
                    List.of("鸡蛋打散后下锅炒至凝固。", "番茄切块炒出汁。", "倒回鸡蛋，加盐和少量糖调味。")
            ),
            new Recipe(
                    "蒜蓉西兰花",
                    List.of("西兰花", "蒜", "盐", "食用油"),
                    List.of("西兰花切小朵后焯水。", "热油炒香蒜末。", "加入西兰花快速翻炒并加盐调味。")
            ),
            new Recipe(
                    "土豆炖牛肉",
                    List.of("牛肉", "土豆", "胡萝卜", "姜", "生抽"),
                    List.of("牛肉切块焯水去浮沫。", "加入姜片和调料炖煮。", "放入土豆和胡萝卜炖至软烂。")
            )
    );

    public List<Recipe> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return recipes;
        }

        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return recipes.stream()
                .filter(recipe -> matches(recipe, normalizedKeyword))
                .toList();
    }

    private boolean matches(Recipe recipe, String keyword) {
        return contains(recipe.name(), keyword)
                || recipe.ingredients().stream().anyMatch(value -> contains(value, keyword))
                || recipe.steps().stream().anyMatch(value -> contains(value, keyword));
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
