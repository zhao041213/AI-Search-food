package com.example.food.user.healthnutrition;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HealthNutritionProfileMapper extends BaseMapper<HealthNutritionProfile> {

    @Select("SELECT * FROM user_health_nutrition_profiles WHERE user_id = #{userId}")
    HealthNutritionProfile findByUserId(@Param("userId") Long userId);

    @Update("""
            UPDATE user_health_nutrition_profiles
            SET gender = #{profile.gender}, age = #{profile.age}, height_cm = #{profile.heightCm},
                weight_kg = #{profile.weightKg}, activity_level = #{profile.activityLevel}, goal = #{profile.goal},
                dietary_restrictions_json = #{profile.dietaryRestrictionsJson}, allergies_json = #{profile.allergiesJson},
                special_health_condition = #{profile.specialHealthCondition}, target_mode = #{profile.targetMode},
                ai_calories_kcal = #{profile.aiCaloriesKcal}, ai_protein_g = #{profile.aiProteinG},
                ai_fat_g = #{profile.aiFatG}, ai_carbohydrate_g = #{profile.aiCarbohydrateG},
                ai_reference_basis = #{profile.aiReferenceBasis}, ai_reference_updated_at = #{profile.aiReferenceUpdatedAt},
                current_calories_kcal = #{profile.currentCaloriesKcal}, current_protein_g = #{profile.currentProteinG},
                current_fat_g = #{profile.currentFatG}, current_carbohydrate_g = #{profile.currentCarbohydrateG},
                updated_at = #{profile.updatedAt}
            WHERE user_id = #{profile.userId}
            """)
    int updateByUserId(@Param("profile") HealthNutritionProfile profile);
}
