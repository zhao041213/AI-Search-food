package com.example.food.recipe;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.food.recipe.dto.RecentSearchResponse;
import com.example.food.stats.IngredientNormalizer;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class SearchHistoryService {

    private static final int CANDIDATE_LIMIT = 50;
    private static final int RESULT_LIMIT = 5;

    private final SearchLogMapper searchLogMapper;
    private final IngredientNormalizer ingredientNormalizer;

    public SearchHistoryService(
            SearchLogMapper searchLogMapper,
            IngredientNormalizer ingredientNormalizer
    ) {
        this.searchLogMapper = searchLogMapper;
        this.ingredientNormalizer = ingredientNormalizer;
    }

    public List<RecentSearchResponse> recent(Long userId) {
        List<SearchLog> candidates = searchLogMapper.selectList(new QueryWrapper<SearchLog>()
                .eq("user_id", userId)
                .orderByDesc("created_at")
                .orderByDesc("id")
                .last("LIMIT " + CANDIDATE_LIMIT));

        Set<String> seen = new HashSet<>();
        return candidates.stream()
                .filter(searchLog -> seen.add(deduplicationKey(searchLog)))
                .limit(RESULT_LIMIT)
                .map(this::toResponse)
                .toList();
    }

    private String deduplicationKey(SearchLog searchLog) {
        String ingredients = ingredientNormalizer.normalizeDistinct(searchLog.getQueryText()).stream()
                .map(IngredientNormalizer.NormalizedIngredient::canonicalName)
                .map(this::normalizeKeyPart)
                .sorted()
                .distinct()
                .reduce((left, right) -> left + "\u001f" + right)
                .orElse("");
        return ingredients
                + "\u001e" + normalizeKeyPart(searchLog.getMealType())
                + "\u001e" + normalizeKeyPart(searchLog.getGoal());
    }

    private RecentSearchResponse toResponse(SearchLog searchLog) {
        return new RecentSearchResponse(
                searchLog.getId(),
                searchLog.getQueryText(),
                searchLog.getMealType(),
                searchLog.getGoal(),
                searchLog.getInputType(),
                searchLog.getCreatedAt()
        );
    }

    private String normalizeKeyPart(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
