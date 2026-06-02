package com.example.food.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeServiceTest {

    private final RecipeService recipeService = new RecipeService();

    @Test
    void searchReturnsAllRecipesWhenKeywordIsBlank() {
        assertThat(recipeService.search("")).hasSizeGreaterThanOrEqualTo(3);
        assertThat(recipeService.search(null)).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void searchMatchesNameIngredientsAndSteps() {
        assertThat(recipeService.search("鸡蛋"))
                .extracting("name")
                .contains("番茄炒蛋");

        assertThat(recipeService.search("焯水"))
                .extracting("name")
                .contains("蒜蓉西兰花");
    }

    @Test
    void searchReturnsEmptyListWhenNoRecipeMatches() {
        assertThat(recipeService.search("不存在的菜")).isEmpty();
    }
}
