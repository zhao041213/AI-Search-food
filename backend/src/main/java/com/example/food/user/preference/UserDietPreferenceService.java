package com.example.food.user.preference;

import com.example.food.user.preference.dto.DietPreferenceRequest;
import com.example.food.user.preference.dto.DietPreferenceResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class UserDietPreferenceService {

    private static final String DEFAULT_TASTE = "any";
    private static final String DEFAULT_GOAL = "balanced";
    private static final int MAX_INGREDIENTS = 20;
    private static final int MAX_INGREDIENT_LENGTH = 32;

    private final UserDietPreferenceMapper mapper;
    private final ObjectMapper objectMapper;

    public UserDietPreferenceService(UserDietPreferenceMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public DietPreferenceResponse get(Long userId) {
        UserDietPreference preference = mapper.selectById(userId);
        if (preference == null) {
            return DietPreferenceResponse.empty();
        }
        return toResponse(preference);
    }

    @Transactional
    public DietPreferenceResponse save(Long userId, DietPreferenceRequest request) {
        List<String> avoidIngredients = normalizeIngredients(request.avoidIngredients());
        List<String> allergenIngredients = normalizeIngredients(request.allergenIngredients());
        UserDietPreference preference = mapper.selectById(userId);
        boolean exists = preference != null;
        LocalDateTime now = LocalDateTime.now();
        if (!exists) {
            preference = new UserDietPreference();
            preference.setUserId(userId);
            preference.setCreatedAt(now);
        }
        preference.setTaste(defaultIfBlank(request.taste(), DEFAULT_TASTE));
        preference.setDefaultGoal(defaultIfBlank(request.defaultGoal(), DEFAULT_GOAL));
        preference.setAvoidIngredients(writeIngredients(avoidIngredients));
        preference.setAllergenIngredients(writeIngredients(allergenIngredients));
        preference.setUpdatedAt(now);

        if (exists) {
            mapper.updateById(preference);
        } else {
            try {
                mapper.insert(preference);
            } catch (DuplicateKeyException exception) {
                preference.setCreatedAt(null);
                mapper.updateById(preference);
            }
        }
        return new DietPreferenceResponse(
                preference.getTaste(),
                preference.getDefaultGoal(),
                avoidIngredients,
                allergenIngredients
        );
    }

    private DietPreferenceResponse toResponse(UserDietPreference preference) {
        return new DietPreferenceResponse(
                defaultIfBlank(preference.getTaste(), DEFAULT_TASTE),
                defaultIfBlank(preference.getDefaultGoal(), DEFAULT_GOAL),
                readIngredients(preference.getAvoidIngredients()),
                readIngredients(preference.getAllergenIngredients())
        );
    }

    private List<String> normalizeIngredients(List<String> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String ingredient : ingredients) {
            if (ingredient == null) {
                continue;
            }
            String trimmed = ingredient.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() > MAX_INGREDIENT_LENGTH) {
                throw new IllegalArgumentException("食材名称不能超过 32 个字符");
            }
            normalized.add(trimmed);
        }
        if (normalized.size() > MAX_INGREDIENTS) {
            throw new IllegalArgumentException("食材列表不能超过 20 项");
        }
        return List.copyOf(normalized);
    }

    private String writeIngredients(List<String> ingredients) {
        try {
            return objectMapper.writeValueAsString(ingredients);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("饮食偏好序列化失败", exception);
        }
    }

    private List<String> readIngredients(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            List<String> ingredients = objectMapper.readValue(value, new TypeReference<>() {
            });
            return ingredients == null ? List.of() : List.copyOf(ingredients);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("饮食偏好数据格式错误", exception);
        }
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
