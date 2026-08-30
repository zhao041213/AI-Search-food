package com.example.food.user.health;

import com.example.food.user.health.dto.HealthProfileRequest;
import com.example.food.user.health.dto.HealthProfileResponse;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserHealthProfileServiceTest {

    @Mock
    private UserHealthProfileMapper mapper;

    private UserHealthProfileService service;

    @BeforeEach
    void setUp() {
        service = new UserHealthProfileService(mapper);
    }

    @Test
    void returnsEmptyProfileWhenUserHasNotSavedOne() {
        when(mapper.selectById(7L)).thenReturn(null);

        HealthProfileResponse response = service.get(7L);

        assertThat(response).isEqualTo(HealthProfileResponse.empty());
    }

    @Test
    void savesProfileAndCalculatesBmi() {
        when(mapper.selectById(7L)).thenReturn(null);

        HealthProfileResponse response = service.save(7L, request());

        ArgumentCaptor<UserHealthProfile> captor = ArgumentCaptor.forClass(UserHealthProfile.class);
        verify(mapper).insert(captor.capture());
        UserHealthProfile saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getAgeRange()).isEqualTo("AGE_30_44");
        assertThat(saved.getHeightCm()).isEqualByComparingTo("172.0");
        assertThat(saved.getWeightKg()).isEqualByComparingTo("64.0");
        assertThat(saved.getActivityLevel()).isEqualTo("MODERATE");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(response.completed()).isTrue();
        assertThat(response.bmi()).isEqualByComparingTo("21.6");
    }

    @Test
    void updatesExistingProfileWithoutReplacingCreatedAt() {
        UserHealthProfile existing = profile(7L);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 12, 0);
        existing.setCreatedAt(createdAt);
        when(mapper.selectById(7L)).thenReturn(existing);
        HealthProfileRequest request = new HealthProfileRequest(
                "AGE_45_59",
                new BigDecimal("165.0"),
                new BigDecimal("70.0"),
                "LOW"
        );

        HealthProfileResponse response = service.save(7L, request);

        verify(mapper).updateById(existing);
        assertThat(existing.getCreatedAt()).isEqualTo(createdAt);
        assertThat(existing.getAgeRange()).isEqualTo("AGE_45_59");
        assertThat(existing.getActivityLevel()).isEqualTo("LOW");
        assertThat(response.bmi()).isEqualByComparingTo("25.7");
    }

    @Test
    void usesUpdateWhenConcurrentFirstSaveAlreadyInsertedTheProfile() {
        when(mapper.selectById(7L)).thenReturn(null);
        when(mapper.insert(org.mockito.ArgumentMatchers.any(UserHealthProfile.class)))
                .thenThrow(new DuplicateKeyException("duplicate user_id"));

        service.save(7L, request());

        verify(mapper).updateById(org.mockito.ArgumentMatchers.<UserHealthProfile>argThat(profile ->
                profile.getUserId().equals(7L)
                        && profile.getCreatedAt() == null
                        && profile.getActivityLevel().equals("MODERATE")
        ));
    }

    @Test
    void exposesStructuredProfileForRecipeRecommendation() {
        when(mapper.selectById(7L)).thenReturn(profile(7L));

        UserHealthProfileService.RecommendationContext context = service.getRecommendationContext(7L);

        assertThat(context.ageRange()).isEqualTo("AGE_30_44");
        assertThat(context.activityLevel()).isEqualTo("MODERATE");
        assertThat(context.bmi()).isEqualByComparingTo("21.6");
    }

    @Test
    void deletesOnlyTheCurrentUsersProfile() {
        service.delete(7L);

        verify(mapper).deleteById(7L);
    }

    private HealthProfileRequest request() {
        return new HealthProfileRequest(
                "AGE_30_44",
                new BigDecimal("172.0"),
                new BigDecimal("64.0"),
                "MODERATE"
        );
    }

    private UserHealthProfile profile(Long userId) {
        UserHealthProfile profile = new UserHealthProfile();
        profile.setUserId(userId);
        profile.setAgeRange("AGE_30_44");
        profile.setHeightCm(new BigDecimal("172.0"));
        profile.setWeightKg(new BigDecimal("64.0"));
        profile.setActivityLevel("MODERATE");
        profile.setUpdatedAt(LocalDateTime.of(2026, 8, 1, 12, 0));
        return profile;
    }
}
