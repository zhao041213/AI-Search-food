package com.example.food.shopping;

import com.example.food.recipe.RecipeRecordMapper;
import com.example.food.recipe.SearchLog;
import com.example.food.recipe.SearchLogMapper;
import com.example.food.shopping.dto.ShoppingItemCheckRequest;
import com.example.food.shopping.dto.ShoppingItemCheckResponse;
import com.example.food.stats.IngredientNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingItemCheckServiceTest {

    @Mock
    private ShoppingItemCheckMapper mapper;

    @Mock
    private SearchLogMapper searchLogMapper;

    @Mock
    private RecipeRecordMapper recipeRecordMapper;

    private ShoppingItemCheckService service;

    @BeforeEach
    void setUp() {
        service = new ShoppingItemCheckService(
                mapper,
                searchLogMapper,
                recipeRecordMapper,
                new IngredientNormalizer()
        );
    }

    @Test
    void savesNormalizedCheckForOwnedSearchLog() {
        when(searchLogMapper.selectById(88L)).thenReturn(searchLog(88L, 7L));
        when(mapper.findByIdentity(7L, 88L, "番茄")).thenReturn(null);

        ShoppingItemCheckResponse response = service.save(7L, new ShoppingItemCheckRequest(
                88L, "西红柿", true
        ));

        ArgumentCaptor<ShoppingItemCheck> captor = ArgumentCaptor.forClass(ShoppingItemCheck.class);
        verify(mapper).insert(captor.capture());
        ShoppingItemCheck saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getSearchLogId()).isEqualTo(88L);
        assertThat(saved.getIngredientName()).isEqualTo("番茄");
        assertThat(saved.isChecked()).isTrue();
        assertThat(response).isEqualTo(new ShoppingItemCheckResponse("番茄", true));
    }

    @Test
    void savesPurchasingStatusWithoutMarkingItemReady() {
        when(searchLogMapper.selectById(88L)).thenReturn(searchLog(88L, 7L));
        when(mapper.findByIdentity(7L, 88L, "egg")).thenReturn(null);

        ShoppingItemCheckResponse response = service.save(7L, new ShoppingItemCheckRequest(
                88L, "egg", "PURCHASING"
        ));

        ArgumentCaptor<ShoppingItemCheck> captor = ArgumentCaptor.forClass(ShoppingItemCheck.class);
        verify(mapper).insert(captor.capture());
        ShoppingItemCheck saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("PURCHASING");
        assertThat(saved.isChecked()).isFalse();
        assertThat(response.status()).isEqualTo("PURCHASING");
        assertThat(response.checked()).isFalse();
    }

    @Test
    void allowsSavedRecipeToReuseAnonymousSearchLog() {
        when(searchLogMapper.selectById(88L)).thenReturn(searchLog(88L, null));
        when(recipeRecordMapper.existsByUserIdAndSearchLogId(7L, 88L)).thenReturn(true);
        when(mapper.findByUserIdAndSearchLogId(7L, 88L)).thenReturn(List.of(check(7L, 88L, "鸡蛋", true)));

        List<ShoppingItemCheckResponse> checks = service.list(7L, 88L);

        assertThat(checks).containsExactly(new ShoppingItemCheckResponse("鸡蛋", true));
    }

    @Test
    void rejectsSearchLogBelongingToAnotherUser() {
        when(searchLogMapper.selectById(88L)).thenReturn(searchLog(88L, 8L));
        when(recipeRecordMapper.existsByUserIdAndSearchLogId(7L, 88L)).thenReturn(false);

        assertThatThrownBy(() -> service.list(7L, 88L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("无权使用该采购清单");
    }

    @Test
    void updatesExistingCheckInsteadOfInsertingAgain() {
        when(searchLogMapper.selectById(88L)).thenReturn(searchLog(88L, 7L));
        ShoppingItemCheck existing = check(7L, 88L, "鸡蛋", true);
        when(mapper.findByIdentity(7L, 88L, "鸡蛋")).thenReturn(existing);

        ShoppingItemCheckResponse response = service.save(7L, new ShoppingItemCheckRequest(
                88L, "鸡蛋", false
        ));

        verify(mapper).updateById(existing);
        assertThat(existing.isChecked()).isFalse();
        assertThat(response.checked()).isFalse();
    }

    private SearchLog searchLog(Long id, Long userId) {
        SearchLog searchLog = new SearchLog();
        searchLog.setId(id);
        searchLog.setUserId(userId);
        return searchLog;
    }

    private ShoppingItemCheck check(Long userId, Long searchLogId, String ingredientName, boolean checked) {
        ShoppingItemCheck item = new ShoppingItemCheck();
        item.setUserId(userId);
        item.setSearchLogId(searchLogId);
        item.setIngredientName(ingredientName);
        item.setChecked(checked);
        return item;
    }
}
