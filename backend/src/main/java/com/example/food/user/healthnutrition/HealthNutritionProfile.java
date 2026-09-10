package com.example.food.user.healthnutrition;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("user_health_nutrition_profiles")
public class HealthNutritionProfile {

    @TableId(type = IdType.INPUT)
    private Long userId;
    private String gender;
    private Integer age;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private String activityLevel;
    private String goal;
    private String dietaryRestrictionsJson;
    private String allergiesJson;
    private String specialHealthCondition;
    private String targetMode;
    private BigDecimal aiCaloriesKcal;
    private BigDecimal aiProteinG;
    private BigDecimal aiFatG;
    private BigDecimal aiCarbohydrateG;
    private String aiReferenceBasis;
    private LocalDateTime aiReferenceUpdatedAt;
    private BigDecimal currentCaloriesKcal;
    private BigDecimal currentProteinG;
    private BigDecimal currentFatG;
    private BigDecimal currentCarbohydrateG;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public BigDecimal getHeightCm() { return heightCm; }
    public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public String getDietaryRestrictionsJson() { return dietaryRestrictionsJson; }
    public void setDietaryRestrictionsJson(String dietaryRestrictionsJson) { this.dietaryRestrictionsJson = dietaryRestrictionsJson; }
    public String getAllergiesJson() { return allergiesJson; }
    public void setAllergiesJson(String allergiesJson) { this.allergiesJson = allergiesJson; }
    public String getSpecialHealthCondition() { return specialHealthCondition; }
    public void setSpecialHealthCondition(String specialHealthCondition) { this.specialHealthCondition = specialHealthCondition; }
    public String getTargetMode() { return targetMode; }
    public void setTargetMode(String targetMode) { this.targetMode = targetMode; }
    public BigDecimal getAiCaloriesKcal() { return aiCaloriesKcal; }
    public void setAiCaloriesKcal(BigDecimal aiCaloriesKcal) { this.aiCaloriesKcal = aiCaloriesKcal; }
    public BigDecimal getAiProteinG() { return aiProteinG; }
    public void setAiProteinG(BigDecimal aiProteinG) { this.aiProteinG = aiProteinG; }
    public BigDecimal getAiFatG() { return aiFatG; }
    public void setAiFatG(BigDecimal aiFatG) { this.aiFatG = aiFatG; }
    public BigDecimal getAiCarbohydrateG() { return aiCarbohydrateG; }
    public void setAiCarbohydrateG(BigDecimal aiCarbohydrateG) { this.aiCarbohydrateG = aiCarbohydrateG; }
    public String getAiReferenceBasis() { return aiReferenceBasis; }
    public void setAiReferenceBasis(String aiReferenceBasis) { this.aiReferenceBasis = aiReferenceBasis; }
    public LocalDateTime getAiReferenceUpdatedAt() { return aiReferenceUpdatedAt; }
    public void setAiReferenceUpdatedAt(LocalDateTime aiReferenceUpdatedAt) { this.aiReferenceUpdatedAt = aiReferenceUpdatedAt; }
    public BigDecimal getCurrentCaloriesKcal() { return currentCaloriesKcal; }
    public void setCurrentCaloriesKcal(BigDecimal currentCaloriesKcal) { this.currentCaloriesKcal = currentCaloriesKcal; }
    public BigDecimal getCurrentProteinG() { return currentProteinG; }
    public void setCurrentProteinG(BigDecimal currentProteinG) { this.currentProteinG = currentProteinG; }
    public BigDecimal getCurrentFatG() { return currentFatG; }
    public void setCurrentFatG(BigDecimal currentFatG) { this.currentFatG = currentFatG; }
    public BigDecimal getCurrentCarbohydrateG() { return currentCarbohydrateG; }
    public void setCurrentCarbohydrateG(BigDecimal currentCarbohydrateG) { this.currentCarbohydrateG = currentCarbohydrateG; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
