package com.example.food.user.healthnutrition;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthNutritionServiceTest {

    private final HealthNutritionProfileMapper mapper = mock(HealthNutritionProfileMapper.class);
    private final HealthNutritionService service = new HealthNutritionService(mapper, new ObjectMapper());

    @Test
    void recommendationContextContainsDietaryBoundariesAndCurrentTarget() {
        HealthNutritionProfile profile = profile();
        profile.setTargetMode("CUSTOM");
        profile.setCurrentCaloriesKcal(new BigDecimal("2000"));
        profile.setCurrentProteinG(new BigDecimal("90"));
        profile.setCurrentFatG(new BigDecimal("65"));
        profile.setCurrentCarbohydrateG(new BigDecimal("240"));
        when(mapper.findByUserId(7L)).thenReturn(profile);

        HealthNutritionService.RecommendationContext context = service.recommendationContext(7L);

        assertThat(context.gender()).isEqualTo("FEMALE");
        assertThat(context.dietaryRestrictions()).containsExactly("乳糖不耐");
        assertThat(context.allergies()).containsExactly("花生");
        assertThat(context.target().caloriesKcal()).isEqualByComparingTo("2000");
    }

    @Test
    void disabledTargetKeepsHealthContextButOmitsNutritionTarget() {
        HealthNutritionProfile profile = profile();
        profile.setTargetMode("DISABLED");
        when(mapper.findByUserId(7L)).thenReturn(profile);

        var context = service.recommendationContext(7L);
        assertThat(context).isNotNull();
        assertThat(context.target()).isNull();
    }

    private HealthNutritionProfile profile() {
        HealthNutritionProfile profile = new HealthNutritionProfile();
        profile.setUserId(7L);
        profile.setGender("FEMALE");
        profile.setAge(32);
        profile.setHeightCm(new BigDecimal("168"));
        profile.setWeightKg(new BigDecimal("58"));
        profile.setActivityLevel("MODERATE");
        profile.setGoal("MAINTAIN");
        profile.setDietaryRestrictionsJson("[\"乳糖不耐\"]");
        profile.setAllergiesJson("[\"花生\"]");
        profile.setSpecialHealthCondition("无");
        return profile;
    }
}
