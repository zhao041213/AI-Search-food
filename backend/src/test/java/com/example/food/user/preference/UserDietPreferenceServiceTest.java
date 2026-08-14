package com.example.food.user.preference;

import com.example.food.user.preference.dto.DietPreferenceRequest;
import com.example.food.user.preference.dto.DietPreferenceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDietPreferenceServiceTest {

    @Mock
    private UserDietPreferenceMapper mapper;

    private UserDietPreferenceService service;

    @BeforeEach
    void setUp() {
        service = new UserDietPreferenceService(mapper, new ObjectMapper());
    }

    @Test
    void returnsDefaultsWhenUserHasNoPreference() {
        when(mapper.selectById(7L)).thenReturn(null);

        DietPreferenceResponse result = service.get(7L);

        assertThat(result).isEqualTo(new DietPreferenceResponse(
                "any",
                "balanced",
                List.of(),
                List.of()
        ));
    }

    @Test
    void readsStoredJsonIngredientLists() {
        UserDietPreference preference = preference(7L);
        preference.setTaste("light");
        preference.setDefaultGoal("fat_loss");
        preference.setAvoidIngredients("[\"香菜\",\"葱\"]");
        preference.setAllergenIngredients("[\"花生\"]");
        when(mapper.selectById(7L)).thenReturn(preference);

        DietPreferenceResponse result = service.get(7L);

        assertThat(result.taste()).isEqualTo("light");
        assertThat(result.defaultGoal()).isEqualTo("fat_loss");
        assertThat(result.avoidIngredients()).containsExactly("香菜", "葱");
        assertThat(result.allergenIngredients()).containsExactly("花生");
    }

    @Test
    void normalizesAndInsertsNewPreference() {
        when(mapper.selectById(7L)).thenReturn(null);
        DietPreferenceRequest request = new DietPreferenceRequest(
                "light",
                "balanced",
                Arrays.asList(" 香菜 ", "香菜", "", "  ", null, "葱"),
                List.of(" 花生 ", "花生")
        );

        DietPreferenceResponse result = service.save(7L, request);

        ArgumentCaptor<UserDietPreference> captor = ArgumentCaptor.forClass(UserDietPreference.class);
        verify(mapper).insert(captor.capture());
        UserDietPreference saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getTaste()).isEqualTo("light");
        assertThat(saved.getDefaultGoal()).isEqualTo("balanced");
        assertThat(saved.getAvoidIngredients()).isEqualTo("[\"香菜\",\"葱\"]");
        assertThat(saved.getAllergenIngredients()).isEqualTo("[\"花生\"]");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(result.avoidIngredients()).containsExactly("香菜", "葱");
    }

    @Test
    void overwritesExistingPreference() {
        UserDietPreference existing = preference(7L);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 12, 0);
        existing.setCreatedAt(createdAt);
        when(mapper.selectById(7L)).thenReturn(existing);

        DietPreferenceResponse result = service.save(7L, new DietPreferenceRequest(
                "spicy",
                "muscle_gain",
                List.of("香菜"),
                List.of("牛奶")
        ));

        ArgumentCaptor<UserDietPreference> captor = ArgumentCaptor.forClass(UserDietPreference.class);
        verify(mapper).updateById(captor.capture());
        UserDietPreference saved = captor.getValue();
        assertThat(saved).isSameAs(existing);
        assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
        assertThat(saved.getUpdatedAt()).isAfter(createdAt);
        assertThat(saved.getAvoidIngredients()).isEqualTo("[\"香菜\"]");
        assertThat(saved.getAllergenIngredients()).isEqualTo("[\"牛奶\"]");
        assertThat(result.taste()).isEqualTo("spicy");
        assertThat(result.defaultGoal()).isEqualTo("muscle_gain");
    }

    @Test
    void updatesWhenConcurrentFirstSaveAlreadyInsertedThePreference() {
        when(mapper.selectById(7L)).thenReturn(null);
        when(mapper.insert(org.mockito.ArgumentMatchers.any(UserDietPreference.class)))
                .thenThrow(new DuplicateKeyException("duplicate user_id"));
        DietPreferenceRequest request = new DietPreferenceRequest(
                "light",
                "fat_loss",
                List.of("香菜"),
                List.of("花生")
        );

        DietPreferenceResponse result = service.save(7L, request);

        verify(mapper).updateById(org.mockito.ArgumentMatchers.<UserDietPreference>argThat(preference ->
                preference.getUserId().equals(7L)
                        && preference.getDefaultGoal().equals("fat_loss")
                        && preference.getCreatedAt() == null
        ));
        assertThat(result.defaultGoal()).isEqualTo("fat_loss");
    }

    private UserDietPreference preference(Long userId) {
        UserDietPreference preference = new UserDietPreference();
        preference.setUserId(userId);
        preference.setAvoidIngredients("[]");
        preference.setAllergenIngredients("[]");
        return preference;
    }
}
