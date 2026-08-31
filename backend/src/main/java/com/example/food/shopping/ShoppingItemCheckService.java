package com.example.food.shopping;

import com.example.food.recipe.RecipeRecordMapper;
import com.example.food.recipe.SearchLog;
import com.example.food.recipe.SearchLogMapper;
import com.example.food.shopping.dto.ShoppingItemCheckRequest;
import com.example.food.shopping.dto.ShoppingItemCheckResponse;
import com.example.food.stats.IngredientNormalizer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingItemCheckService {

    private final ShoppingItemCheckMapper mapper;
    private final SearchLogMapper searchLogMapper;
    private final RecipeRecordMapper recipeRecordMapper;
    private final IngredientNormalizer ingredientNormalizer;

    public ShoppingItemCheckService(
            ShoppingItemCheckMapper mapper,
            SearchLogMapper searchLogMapper,
            RecipeRecordMapper recipeRecordMapper,
            IngredientNormalizer ingredientNormalizer
    ) {
        this.mapper = mapper;
        this.searchLogMapper = searchLogMapper;
        this.recipeRecordMapper = recipeRecordMapper;
        this.ingredientNormalizer = ingredientNormalizer;
    }

    public List<ShoppingItemCheckResponse> list(Long userId, Long searchLogId) {
        verifySearchLogAccess(userId, searchLogId);
        return mapper.findByUserIdAndSearchLogId(userId, searchLogId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ShoppingItemCheckResponse save(Long userId, ShoppingItemCheckRequest request) {
        verifySearchLogAccess(userId, request.searchLogId());
        String ingredientName = normalizeIngredientName(request.ingredientName());
        ShoppingItemStatus status = ShoppingItemStatus.from(request.status(), request.checked());
        ShoppingItemCheck item = mapper.findByIdentity(userId, request.searchLogId(), ingredientName);
        LocalDateTime now = LocalDateTime.now();
        if (item == null) {
            item = new ShoppingItemCheck();
            item.setUserId(userId);
            item.setSearchLogId(request.searchLogId());
            item.setIngredientName(ingredientName);
            item.setStatus(status.name());
            item.setChecked(status.isReady());
            item.setCreatedAt(now);
            item.setUpdatedAt(now);
            mapper.insert(item);
        } else {
            item.setStatus(status.name());
            item.setChecked(status.isReady());
            item.setUpdatedAt(now);
            mapper.updateById(item);
        }
        return toResponse(item);
    }

    private void verifySearchLogAccess(Long userId, Long searchLogId) {
        SearchLog searchLog = searchLogMapper.selectById(searchLogId);
        if (searchLog == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "搜索记录不存在");
        }
        if (userId.equals(searchLog.getUserId())
                || recipeRecordMapper.existsByUserIdAndSearchLogId(userId, searchLogId)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权使用该采购清单");
    }

    private String normalizeIngredientName(String value) {
        List<IngredientNormalizer.NormalizedIngredient> ingredients =
                ingredientNormalizer.normalizeDistinct(value);
        if (ingredients.size() != 1) {
            throw new IllegalArgumentException("食材名称只能填写一种食材");
        }
        return ingredients.get(0).canonicalName();
    }

    private ShoppingItemCheckResponse toResponse(ShoppingItemCheck item) {
        ShoppingItemStatus status = ShoppingItemStatus.from(item.getStatus(), item.isChecked());
        return new ShoppingItemCheckResponse(item.getIngredientName(), status.name(), status.isReady());
    }
}
