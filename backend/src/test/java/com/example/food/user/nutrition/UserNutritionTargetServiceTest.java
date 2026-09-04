package com.example.food.user.nutrition;

import com.example.food.user.nutrition.dto.NutritionTargetRequest;
import com.example.food.user.nutrition.dto.NutritionTargetResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserNutritionTargetServiceTest {

    @Mock
    private UserNutritionTargetMapper mapper;

    private UserNutritionTargetService service;

    @BeforeEach
    void setUp() {
        service = new UserNutritionTargetService(mapper);
    }

    @Test
    void returnsEmptyTargetWhenUserHasNotConfiguredOne() {
        when(mapper.selectById(7L)).thenReturn(null);

        assertThat(service.get(7L)).isEqualTo(NutritionTargetResponse.empty());
    }

    @Test
    void savesCompleteTargetForTheCurrentUser() {
        when(mapper.selectById(7L)).thenReturn(null);

        NutritionTargetResponse response = service.save(7L, request(true));

        ArgumentCaptor<UserNutritionTarget> captor = ArgumentCaptor.forClass(UserNutritionTarget.class);
        verify(mapper).insert(captor.capture());
        UserNutritionTarget saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getCaloriesKcal()).isEqualByComparingTo("2000");
        assertThat(saved.getProteinG()).isEqualByComparingTo("80");
        assertThat(response.configured()).isTrue();
        assertThat(response.enabled()).isTrue();
    }

    @Test
    void disabledTargetClearsStoredValuesAndIsIgnoredByRecommendationContext() {
        UserNutritionTarget existing = target(7L);
        when(mapper.selectById(7L)).thenReturn(existing);

        NutritionTargetResponse response = service.save(7L, new NutritionTargetRequest(false, null, null, null, null));

        verify(mapper).updateById(existing);
        assertThat(existing.getEnabled()).isFalse();
        assertThat(existing.getCaloriesKcal()).isNull();
        assertThat(response.enabled()).isFalse();
        assertThat(service.getRecommendationContext(7L)).isNull();
    }

    @Test
    void rejectsIncompleteOrOutOfRangeEnabledTargetWithChineseMessage() {
        assertThatThrownBy(() -> service.save(7L, new NutritionTargetRequest(
                true,
                new BigDecimal("10001"),
                new BigDecimal("80"),
                new BigDecimal("60"),
                null
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("每日热量目标不能超过 10000");
    }

    @Test
    void ignoresInvalidPersistedTargetInsteadOfInjectingItIntoPrompt() {
        UserNutritionTarget invalid = target(7L);
        invalid.setProteinG(null);
        when(mapper.selectById(7L)).thenReturn(invalid);

        assertThat(service.getRecommendationContext(7L)).isNull();
    }

    @Test
    void concurrentFirstSaveFallsBackToUpdate() {
        when(mapper.selectById(7L)).thenReturn(null);
        when(mapper.insert(any(UserNutritionTarget.class)))
                .thenThrow(new DuplicateKeyException("duplicate user_id"));

        service.save(7L, request(true));

        verify(mapper).updateById(org.mockito.ArgumentMatchers.<UserNutritionTarget>argThat(target ->
                target.getUserId().equals(7L) && target.getCreatedAt() == null && target.getEnabled()
        ));
    }

    @Test
    void deletesOnlyTheCurrentUsersTarget() {
        service.delete(7L);

        verify(mapper).deleteById(7L);
    }

    private NutritionTargetRequest request(boolean enabled) {
        return new NutritionTargetRequest(
                enabled,
                new BigDecimal("2000"),
                new BigDecimal("80"),
                new BigDecimal("60"),
                new BigDecimal("260")
        );
    }

    private UserNutritionTarget target(Long userId) {
        UserNutritionTarget target = new UserNutritionTarget();
        target.setUserId(userId);
        target.setEnabled(true);
        target.setCaloriesKcal(new BigDecimal("2000"));
        target.setProteinG(new BigDecimal("80"));
        target.setFatG(new BigDecimal("60"));
        target.setCarbohydrateG(new BigDecimal("260"));
        target.setUpdatedAt(LocalDateTime.of(2026, 9, 1, 12, 0));
        return target;
    }
}
