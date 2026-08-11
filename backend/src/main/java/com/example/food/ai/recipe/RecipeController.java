package com.example.food.ai.recipe;

import com.example.food.ai.recipe.dto.RecipeGenerateRequest;
import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.example.food.common.ApiResponse;
import com.example.food.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    public ApiResponse<RecipeGenerateResponse> generate(
            @Valid @RequestBody RecipeGenerateRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Anonymous-Id", required = false) String anonymousId
    ) {
        return ApiResponse.ok(recipeRecommendationService.generate(
                request,
                currentPrincipal(authentication),
                normalizeAnonymousId(anonymousId)
        ));
    }

    private AuthPrincipal currentPrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            return null;
        }
        return principal;
    }

    private String normalizeAnonymousId(String anonymousId) {
        if (!StringUtils.hasText(anonymousId)) {
            return null;
        }
        String normalized = anonymousId.trim();
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }
}
