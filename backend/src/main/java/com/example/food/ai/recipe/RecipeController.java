package com.example.food.ai.recipe;

import com.example.food.ai.recipe.dto.RecipeGenerateRequest;
import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.example.food.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/recipes")
public class RecipeController {

    private final RecipeRecommendationService recipeRecommendationService;

    public RecipeController(RecipeRecommendationService recipeRecommendationService) {
        this.recipeRecommendationService = recipeRecommendationService;
    }

    @PostMapping("/generate")
    public ApiResponse<RecipeGenerateResponse> generate(@Valid @RequestBody RecipeGenerateRequest request) {
        return ApiResponse.ok(recipeRecommendationService.generate(request));
    }
}
