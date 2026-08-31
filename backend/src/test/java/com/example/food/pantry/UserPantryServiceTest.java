package com.example.food.pantry;

import com.example.food.pantry.dto.PantryItemRequest;
import com.example.food.pantry.dto.PantryItemResponse;
import com.example.food.stats.IngredientNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPantryServiceTest {

    @Mock
    private UserPantryItemMapper mapper;

    private UserPantryService service;

    @BeforeEach
    void setUp() {
        service = new UserPantryService(mapper, new IngredientNormalizer());
    }

    @Test
    void listsUserInventoryAndIngredientNames() {
        when(mapper.findByUserId(7L)).thenReturn(List.of(
                item(1L, 7L, "番茄"),
                item(2L, 7L, "鸡蛋"),
                item(3L, 7L, "番茄")
        ));

        List<PantryItemResponse> items = service.list(7L);

        assertThat(items).extracting(PantryItemResponse::ingredientName)
                .containsExactly("番茄", "鸡蛋", "番茄");
        assertThat(service.listIngredientNames(7L)).containsExactly("番茄", "鸡蛋");
    }

    @Test
    void summarizesActiveExpiredAndSoonToExpireItems() {
        Clock clock = Clock.fixed(
                LocalDate.of(2026, 8, 31).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant(),
                ZoneId.of("Asia/Shanghai")
        );
        UserPantryService fixedService = new UserPantryService(mapper, new IngredientNormalizer(), clock);

        UserPantryItem expired = item(1L, 7L, "西兰花");
        expired.setQuantity(new BigDecimal("1"));
        expired.setExpireDate(LocalDate.of(2026, 8, 30));
        UserPantryItem soon = item(2L, 7L, "牛奶");
        soon.setQuantity(new BigDecimal("2"));
        soon.setExpireDate(LocalDate.of(2026, 9, 2));
        when(mapper.findExpiryAlertsByUserId(7L, LocalDate.of(2026, 9, 7)))
                .thenReturn(List.of(expired, soon));

        var summary = fixedService.expirySummary(7L);

        assertThat(summary.asOf()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(summary.warningDays()).isEqualTo(7);
        assertThat(summary.expiredItems()).extracting(PantryItemResponse::ingredientName)
                .containsExactly("西兰花");
        assertThat(summary.expiringSoonItems()).extracting(PantryItemResponse::ingredientName)
                .containsExactly("牛奶");
    }

    @Test
    void createsNormalizedPantryItem() {
        PantryItemRequest request = new PantryItemRequest(
                " 西红柿 ",
                "蔬菜",
                new BigDecimal("2.50"),
                "个",
                LocalDate.of(2026, 8, 31)
        );

        PantryItemResponse response = service.create(7L, request);

        ArgumentCaptor<UserPantryItem> captor = ArgumentCaptor.forClass(UserPantryItem.class);
        verify(mapper).insert(captor.capture());
        UserPantryItem saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getIngredientName()).isEqualTo("番茄");
        assertThat(saved.getCategory()).isEqualTo("蔬菜");
        assertThat(saved.getQuantity()).isEqualByComparingTo("2.50");
        assertThat(saved.getUnit()).isEqualTo("个");
        assertThat(saved.getExpireDate()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(response.ingredientName()).isEqualTo("番茄");
    }

    @Test
    void updatesOnlyOwnedPantryItem() {
        UserPantryItem existing = item(5L, 7L, "鸡蛋");
        when(mapper.selectById(5L)).thenReturn(existing);

        PantryItemResponse response = service.update(7L, 5L, new PantryItemRequest(
                "土豆",
                "根茎",
                new BigDecimal("500"),
                "克",
                null
        ));

        verify(mapper).updateById(existing);
        assertThat(existing.getIngredientName()).isEqualTo("土豆");
        assertThat(existing.getCategory()).isEqualTo("根茎");
        assertThat(response.id()).isEqualTo(5L);
    }

    @Test
    void doesNotUpdateAnotherUsersPantryItem() {
        when(mapper.selectById(5L)).thenReturn(item(5L, 8L, "鸡蛋"));

        assertThatThrownBy(() -> service.update(7L, 5L, new PantryItemRequest(
                "鸡蛋", null, null, null, null
        )))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("食材不存在");
    }

    @Test
    void consumesOwnedPantryItemAtomically() {
        UserPantryItem existing = item(5L, 7L, "鸡蛋");
        existing.setQuantity(new BigDecimal("2.50"));
        UserPantryItem updated = item(5L, 7L, "鸡蛋");
        updated.setQuantity(new BigDecimal("1.25"));
        when(mapper.selectById(5L)).thenReturn(existing, updated);
        when(mapper.consumeByUserIdAndId(7L, 5L, new BigDecimal("1.25"))).thenReturn(1);

        PantryItemResponse response = service.consume(7L, 5L, new BigDecimal("1.25"));

        verify(mapper).consumeByUserIdAndId(7L, 5L, new BigDecimal("1.25"));
        assertThat(response.quantity()).isEqualByComparingTo("1.25");
    }

    @Test
    void rejectsConsumptionWhenInventoryIsInsufficient() {
        UserPantryItem existing = item(5L, 7L, "鸡蛋");
        existing.setQuantity(new BigDecimal("1.00"));
        when(mapper.selectById(5L)).thenReturn(existing);
        when(mapper.consumeByUserIdAndId(7L, 5L, new BigDecimal("1.01"))).thenReturn(0);

        assertThatThrownBy(() -> service.consume(7L, 5L, new BigDecimal("1.01")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("库存数量不足");
    }

    @Test
    void rejectsConsumptionWhenInventoryQuantityIsNull() {
        UserPantryItem existing = item(5L, 7L, "鸡蛋");
        when(mapper.selectById(5L)).thenReturn(existing);

        assertThatThrownBy(() -> service.consume(7L, 5L, new BigDecimal("1")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("当前库存数量为空");
    }

    @Test
    void rejectsConsumptionForMissingOrAnotherUsersItem() {
        when(mapper.selectById(5L)).thenReturn(null);
        assertThatThrownBy(() -> service.consume(7L, 5L, new BigDecimal("1")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("食材不存在");

        when(mapper.selectById(6L)).thenReturn(item(6L, 8L, "鸡蛋"));
        assertThatThrownBy(() -> service.consume(7L, 6L, new BigDecimal("1")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("食材不存在");
    }

    @Test
    void rejectsInvalidConsumptionQuantity() {
        assertThatThrownBy(() -> service.consume(7L, 5L, BigDecimal.ZERO))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("必须大于 0");
        assertThatThrownBy(() -> service.consume(7L, 5L, new BigDecimal("1.001")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("最多支持");
    }

    private UserPantryItem item(Long id, Long userId, String ingredientName) {
        UserPantryItem item = new UserPantryItem();
        item.setId(id);
        item.setUserId(userId);
        item.setIngredientName(ingredientName);
        return item;
    }
}
