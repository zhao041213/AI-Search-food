package com.example.food.recipe;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.food.recipe.dto.RecentSearchResponse;
import com.example.food.stats.IngredientNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchHistoryServiceTest {

    @Mock
    private SearchLogMapper searchLogMapper;

    private SearchHistoryService searchHistoryService;

    @BeforeEach
    void setUp() {
        searchHistoryService = new SearchHistoryService(searchLogMapper, new IngredientNormalizer());
    }

    @Test
    void returnsLatestFiveDistinctSearchesForCurrentUser() {
        when(searchLogMapper.selectList(any())).thenReturn(List.of(
                log(9L, "番茄、鸡蛋", "dinner", "balanced", "text", time(9)),
                log(8L, " 鸡蛋，西红柿 ", "dinner", "balanced", "image", time(8)),
                log(7L, "牛肉", "lunch", "muscle_gain", "text", time(7)),
                log(6L, "土豆", "dinner", "balanced", "image", time(6)),
                log(5L, "鸡胸肉", "lunch", "fat_loss", "text", time(5)),
                log(4L, "西兰花", "dinner", "balanced", "text", time(4)),
                log(3L, "豆腐", "breakfast", "balanced", "text", time(3))
        ));

        List<RecentSearchResponse> result = searchHistoryService.recent(7L);

        assertThat(result).extracting(RecentSearchResponse::id)
                .containsExactly(9L, 7L, 6L, 5L, 4L);
        assertThat(result.get(0)).isEqualTo(new RecentSearchResponse(
                9L,
                "番茄、鸡蛋",
                "dinner",
                "balanced",
                "text",
                time(9)
        ));
    }

    @Test
    void keepsSameIngredientsWhenMealTypeOrGoalDiffers() {
        when(searchLogMapper.selectList(any())).thenReturn(List.of(
                log(3L, "番茄、鸡蛋", "dinner", "balanced", "text", time(3)),
                log(2L, "鸡蛋,番茄", "lunch", "balanced", "text", time(2)),
                log(1L, "番茄\n鸡蛋", "dinner", "fat_loss", "text", time(1))
        ));

        List<RecentSearchResponse> result = searchHistoryService.recent(7L);

        assertThat(result).extracting(RecentSearchResponse::id)
                .containsExactly(3L, 2L, 1L);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void queriesOnlyFiftyLatestCandidatesForCurrentUser() {
        when(searchLogMapper.selectList(any())).thenReturn(List.of());

        searchHistoryService.recent(7L);

        ArgumentCaptor<QueryWrapper<SearchLog>> wrapperCaptor =
                ArgumentCaptor.forClass((Class) QueryWrapper.class);
        verify(searchLogMapper).selectList(wrapperCaptor.capture());
        QueryWrapper<SearchLog> wrapper = wrapperCaptor.getValue();
        String sqlSegment = wrapper.getCustomSqlSegment();
        assertThat(wrapper.getParamNameValuePairs()).containsValue(7L);
        assertThat(sqlSegment)
                .contains("user_id")
                .contains("ORDER BY created_at DESC,id DESC")
                .endsWith("LIMIT 50");
    }

    private SearchLog log(
            Long id,
            String ingredients,
            String mealType,
            String goal,
            String searchMode,
            LocalDateTime createdAt
    ) {
        SearchLog searchLog = new SearchLog();
        searchLog.setId(id);
        searchLog.setUserId(7L);
        searchLog.setQueryText(ingredients);
        searchLog.setMealType(mealType);
        searchLog.setGoal(goal);
        searchLog.setInputType(searchMode);
        searchLog.setCreatedAt(createdAt);
        return searchLog;
    }

    private LocalDateTime time(int hour) {
        return LocalDateTime.of(2026, 8, 13, hour, 0);
    }
}
