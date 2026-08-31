package com.example.food.pantry;

import com.example.food.pantry.dto.PantryItemRequest;
import com.example.food.pantry.dto.PantryItemResponse;
import com.example.food.pantry.dto.PantryExpirySummaryResponse;
import com.example.food.stats.IngredientNormalizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserPantryService {

    public static final int EXPIRY_WARNING_DAYS = 7;

    private final UserPantryItemMapper mapper;
    private final IngredientNormalizer ingredientNormalizer;
    private final Clock clock;

    @Autowired
    public UserPantryService(UserPantryItemMapper mapper, IngredientNormalizer ingredientNormalizer) {
        this(mapper, ingredientNormalizer, Clock.systemDefaultZone());
    }

    UserPantryService(UserPantryItemMapper mapper, IngredientNormalizer ingredientNormalizer, Clock clock) {
        this.mapper = mapper;
        this.ingredientNormalizer = ingredientNormalizer;
        this.clock = clock;
    }

    public List<PantryItemResponse> list(Long userId) {
        return mapper.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    public List<String> listIngredientNames(Long userId) {
        return mapper.findByUserId(userId).stream()
                .map(UserPantryItem::getIngredientName)
                .distinct()
                .toList();
    }

    public PantryExpirySummaryResponse expirySummary(Long userId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate warningUntil = today.plusDays(EXPIRY_WARNING_DAYS);
        List<UserPantryItem> alerts = mapper.findExpiryAlertsByUserId(userId, warningUntil);
        List<PantryItemResponse> expiredItems = new ArrayList<>();
        List<PantryItemResponse> expiringSoonItems = new ArrayList<>();

        for (UserPantryItem item : alerts) {
            PantryItemResponse response = toResponse(item);
            if (item.getExpireDate().isBefore(today)) {
                expiredItems.add(response);
            } else {
                expiringSoonItems.add(response);
            }
        }

        return new PantryExpirySummaryResponse(
                today,
                EXPIRY_WARNING_DAYS,
                List.copyOf(expiredItems),
                List.copyOf(expiringSoonItems)
        );
    }

    @Transactional
    public PantryItemResponse create(Long userId, PantryItemRequest request) {
        LocalDateTime now = LocalDateTime.now();
        UserPantryItem item = new UserPantryItem();
        item.setUserId(userId);
        applyRequest(item, request);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        mapper.insert(item);
        return toResponse(item);
    }

    @Transactional
    public PantryItemResponse update(Long userId, Long itemId, PantryItemRequest request) {
        UserPantryItem item = findOwned(userId, itemId);
        applyRequest(item, request);
        item.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(item);
        return toResponse(item);
    }

    @Transactional
    public PantryItemResponse consume(Long userId, Long itemId, BigDecimal quantity) {
        validateConsumeQuantity(quantity);
        UserPantryItem item = findOwned(userId, itemId);
        if (item.getQuantity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前库存数量为空，无法消耗");
        }

        int updatedRows = mapper.consumeByUserIdAndId(userId, itemId, quantity);
        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "库存数量不足");
        }
        return toResponse(mapper.selectById(itemId));
    }

    @Transactional
    public void delete(Long userId, Long itemId) {
        UserPantryItem item = findOwned(userId, itemId);
        mapper.deleteById(item.getId());
    }

    private UserPantryItem findOwned(Long userId, Long itemId) {
        UserPantryItem item = mapper.selectById(itemId);
        if (item == null || !userId.equals(item.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "食材不存在");
        }
        return item;
    }

    private void validateConsumeQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "消耗数量必须大于 0");
        }
        if (quantity.scale() > 2 || quantity.precision() - quantity.scale() > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "消耗数量最多支持 8 位整数和 2 位小数");
        }
    }

    private void applyRequest(UserPantryItem item, PantryItemRequest request) {
        item.setIngredientName(normalizeIngredientName(request.ingredientName()));
        item.setCategory(trimToNull(request.category()));
        item.setQuantity(request.quantity());
        item.setUnit(trimToNull(request.unit()));
        item.setExpireDate(request.expireDate());
    }

    private String normalizeIngredientName(String value) {
        List<IngredientNormalizer.NormalizedIngredient> ingredients =
                ingredientNormalizer.normalizeDistinct(value);
        if (ingredients.size() != 1) {
            throw new IllegalArgumentException("食材名称只能填写一种食材");
        }
        return ingredients.get(0).canonicalName();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private PantryItemResponse toResponse(UserPantryItem item) {
        return new PantryItemResponse(
                item.getId(),
                item.getIngredientName(),
                item.getCategory(),
                item.getQuantity(),
                item.getUnit(),
                item.getExpireDate(),
                item.getUpdatedAt()
        );
    }
}
