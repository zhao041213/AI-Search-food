package com.example.food.recipe;

import com.example.food.ai.recipe.dto.RecipeGenerateRequest;
import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.stats.IngredientNormalizer;
import com.example.food.stats.SearchLogIngredient;
import com.example.food.stats.SearchLogIngredientMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SearchLogService {

    private static final int MAX_PREFERENCE_GOAL_LENGTH = 80;

    private final SearchLogMapper searchLogMapper;
    private final SearchLogIngredientMapper searchLogIngredientMapper;
    private final IngredientNormalizer ingredientNormalizer;
    private final ObjectMapper objectMapper;

    public SearchLogService(
            SearchLogMapper searchLogMapper,
            SearchLogIngredientMapper searchLogIngredientMapper,
            IngredientNormalizer ingredientNormalizer,
            ObjectMapper objectMapper
    ) {
        this.searchLogMapper = searchLogMapper;
        this.searchLogIngredientMapper = searchLogIngredientMapper;
        this.ingredientNormalizer = ingredientNormalizer;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Long record(
            RecipeGenerateRequest request,
            RecipeGenerateResponse response,
            AuthPrincipal principal,
            String anonymousId
    ) {
        List<IngredientNormalizer.NormalizedIngredient> ingredients =
                ingredientNormalizer.normalizeDistinct(request.ingredients());
        LocalDateTime createdAt = LocalDateTime.now();
        SearchLog searchLog = new SearchLog();
        searchLog.setUserId(userId(principal));
        searchLog.setAnonymousId(normalizeAnonymousId(anonymousId));
        searchLog.setQueryText(normalize(request.ingredients()));
        searchLog.setInputType(defaultValue(request.searchMode(), "text"));
        searchLog.setRecognizedIngredients(toJson(ingredients.stream()
                .map(IngredientNormalizer.NormalizedIngredient::originalName)
                .toList()));
        searchLog.setAiModel(modelLabel(response));
        searchLog.setMealType(normalize(request.mealType()));
        searchLog.setGoal(resolveGoal(request));
        searchLog.setCreatedAt(createdAt);
        searchLogMapper.insert(searchLog);
        ingredients.forEach(ingredient -> searchLogIngredientMapper.insert(
                toIngredient(searchLog.getId(), ingredient, createdAt)
        ));
        return searchLog.getId();
    }

    private SearchLogIngredient toIngredient(
            Long searchLogId,
            IngredientNormalizer.NormalizedIngredient ingredient,
            LocalDateTime createdAt
    ) {
        SearchLogIngredient entity = new SearchLogIngredient();
        entity.setSearchLogId(searchLogId);
        entity.setOriginalName(ingredient.originalName());
        entity.setIngredientName(ingredient.canonicalName());
        entity.setCreatedAt(createdAt);
        return entity;
    }

    private Long userId(AuthPrincipal principal) {
        return principal != null && principal.role() == AppRole.USER ? principal.id() : null;
    }

    private String modelLabel(RecipeGenerateResponse response) {
        String provider = normalize(response.provider());
        String model = normalize(response.model());
        if (provider == null) {
            return model;
        }
        if (model == null) {
            return provider;
        }
        return provider + ":" + model;
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("搜索食材记录序列化失败", exception);
        }
    }

    private String defaultValue(String value, String defaultValue) {
        String normalized = normalize(value);
        return normalized == null ? defaultValue : normalized;
    }

    private String resolveGoal(RecipeGenerateRequest request) {
        String goal = normalize(request.goal());
        if (goal != null || request.dietPreference() == null) {
            return goal;
        }
        String preferenceGoal = normalize(request.dietPreference().defaultGoal());
        if (preferenceGoal == null || preferenceGoal.length() <= MAX_PREFERENCE_GOAL_LENGTH) {
            return preferenceGoal;
        }
        return preferenceGoal.substring(0, MAX_PREFERENCE_GOAL_LENGTH);
    }

    private String normalizeAnonymousId(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
