package com.example.food.stats;

import com.example.food.recipe.SearchLog;
import com.example.food.recipe.SearchLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
public class SearchLogIngredientBackfill implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchLogIngredientBackfill.class);
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final SearchLogMapper searchLogMapper;
    private final SearchLogIngredientMapper ingredientMapper;
    private final IngredientNormalizer ingredientNormalizer;
    private final ObjectMapper objectMapper;

    public SearchLogIngredientBackfill(
            SearchLogMapper searchLogMapper,
            SearchLogIngredientMapper ingredientMapper,
            IngredientNormalizer ingredientNormalizer,
            ObjectMapper objectMapper
    ) {
        this.searchLogMapper = searchLogMapper;
        this.ingredientMapper = ingredientMapper;
        this.ingredientNormalizer = ingredientNormalizer;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (SearchLog searchLog : searchLogMapper.findWithoutIngredientDetails()) {
            backfill(searchLog);
        }
    }

    private void backfill(SearchLog searchLog) {
        try {
            List<String> values = objectMapper.readValue(searchLog.getRecognizedIngredients(), STRING_LIST);
            String joined = values == null ? null : values.stream()
                    .filter(Objects::nonNull)
                    .filter(StringUtils::hasText)
                    .reduce((left, right) -> left + "、" + right)
                    .orElse(null);
            if (!StringUtils.hasText(joined)) {
                joined = searchLog.getQueryText();
            }
            LocalDateTime createdAt = searchLog.getCreatedAt() == null
                    ? LocalDateTime.now()
                    : searchLog.getCreatedAt();
            for (IngredientNormalizer.NormalizedIngredient ingredient
                    : ingredientNormalizer.normalizeDistinct(joined)) {
                ingredientMapper.insert(toEntity(searchLog.getId(), ingredient, createdAt));
            }
        } catch (JsonProcessingException exception) {
            LOGGER.warn("跳过无法解析食材 JSON 的历史搜索记录，searchLogId={}", searchLog.getId());
        }
    }

    private SearchLogIngredient toEntity(
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
}
