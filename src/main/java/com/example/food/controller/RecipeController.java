package com.example.food.controller;

import com.example.food.model.GenerateRequest;
import com.example.food.model.Recipe;
import com.example.food.service.DeepSeekApiException;
import com.example.food.service.DeepSeekConfigurationException;
import com.example.food.service.DeepSeekRecipeService;
import com.example.food.service.RecipeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;
    private final DeepSeekRecipeService deepSeekRecipeService;

    public RecipeController(RecipeService recipeService, DeepSeekRecipeService deepSeekRecipeService) {
        this.recipeService = recipeService;
        this.deepSeekRecipeService = deepSeekRecipeService;
    }

    @GetMapping("/search")
    public List<Recipe> search(@RequestParam(defaultValue = "") String keyword) {
        return recipeService.search(keyword);
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generate(@RequestBody(required = false) GenerateRequest request) {
        if (request == null || !StringUtils.hasText(request.ingredients())) {
            return ResponseEntity.badRequest().body(Map.of("error", "请输入食材"));
        }

        String content = deepSeekRecipeService.generateRecipe(request.ingredients());
        return ResponseEntity.ok(Map.of("content", content));
    }

    @ExceptionHandler(DeepSeekConfigurationException.class)
    public ResponseEntity<Map<String, String>> handleConfigurationError(DeepSeekConfigurationException exception) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(DeepSeekApiException.class)
    public ResponseEntity<Map<String, String>> handleDeepSeekError(DeepSeekApiException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", exception.getMessage()));
    }
}
