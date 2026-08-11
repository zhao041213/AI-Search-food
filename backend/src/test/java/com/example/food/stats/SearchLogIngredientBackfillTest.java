package com.example.food.stats;

import com.example.food.recipe.SearchLog;
import com.example.food.recipe.SearchLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchLogIngredientBackfillTest {

    @Mock
    private SearchLogMapper searchLogMapper;

    @Mock
    private SearchLogIngredientMapper ingredientMapper;

    @Test
    void backfillsAliasesOnceAndKeepsTheOriginalSearchTime() {
        LocalDateTime searchTime = LocalDateTime.of(2026, 8, 1, 10, 30);
        SearchLog searchLog = searchLog(8L, "[\"西红柿\",\"番茄\",\"鸡蛋\"]", searchTime);
        when(searchLogMapper.findWithoutIngredientDetails()).thenReturn(List.of(searchLog));
        SearchLogIngredientBackfill backfill = backfill();

        backfill.run(new DefaultApplicationArguments());
        when(searchLogMapper.findWithoutIngredientDetails()).thenReturn(List.of());
        backfill.run(new DefaultApplicationArguments());

        ArgumentCaptor<SearchLogIngredient> captor = ArgumentCaptor.forClass(SearchLogIngredient.class);
        verify(ingredientMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(
                        SearchLogIngredient::getSearchLogId,
                        SearchLogIngredient::getOriginalName,
                        SearchLogIngredient::getIngredientName,
                        SearchLogIngredient::getCreatedAt
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(8L, "西红柿", "番茄", searchTime),
                        org.assertj.core.groups.Tuple.tuple(8L, "鸡蛋", "鸡蛋", searchTime)
                );
    }

    @Test
    void skipsMalformedJsonWithoutBlockingOtherHistoryRows() {
        SearchLog broken = searchLog(9L, "not-json", LocalDateTime.now());
        SearchLog valid = searchLog(10L, "[\"豆腐\"]", LocalDateTime.now());
        when(searchLogMapper.findWithoutIngredientDetails()).thenReturn(List.of(broken, valid));

        backfill().run(new DefaultApplicationArguments());

        ArgumentCaptor<SearchLogIngredient> captor = ArgumentCaptor.forClass(SearchLogIngredient.class);
        verify(ingredientMapper).insert(captor.capture());
        assertThat(captor.getValue().getSearchLogId()).isEqualTo(10L);
        assertThat(captor.getValue().getIngredientName()).isEqualTo("豆腐");
    }

    private SearchLogIngredientBackfill backfill() {
        return new SearchLogIngredientBackfill(
                searchLogMapper,
                ingredientMapper,
                new IngredientNormalizer(),
                new ObjectMapper()
        );
    }

    private SearchLog searchLog(Long id, String ingredientsJson, LocalDateTime createdAt) {
        SearchLog searchLog = new SearchLog();
        searchLog.setId(id);
        searchLog.setRecognizedIngredients(ingredientsJson);
        searchLog.setCreatedAt(createdAt);
        return searchLog;
    }
}
