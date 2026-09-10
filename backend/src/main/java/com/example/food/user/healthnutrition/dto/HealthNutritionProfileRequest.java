package com.example.food.user.healthnutrition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record HealthNutritionProfileRequest(
        @NotBlank(message = "请选择性别")
        @Pattern(regexp = "MALE|FEMALE|OTHER|PREFER_NOT_TO_SAY", message = "性别选项不合法")
        String gender,
        @NotNull(message = "请输入年龄")
        @Min(value = 13, message = "年龄需在 13 到 120 岁之间")
        @Max(value = 120, message = "年龄需在 13 到 120 岁之间")
        Integer age,
        @NotNull(message = "请输入身高")
        @DecimalMin(value = "100.0", message = "身高需在 100 到 250 厘米之间")
        @DecimalMax(value = "250.0", message = "身高需在 100 到 250 厘米之间")
        @Digits(integer = 3, fraction = 1)
        BigDecimal heightCm,
        @NotNull(message = "请输入体重")
        @DecimalMin(value = "25.0", message = "体重需在 25 到 300 千克之间")
        @DecimalMax(value = "300.0", message = "体重需在 25 到 300 千克之间")
        @Digits(integer = 3, fraction = 1)
        BigDecimal weightKg,
        @NotBlank(message = "请选择活动强度")
        @Pattern(regexp = "LOW|MODERATE|HIGH", message = "活动强度选项不合法")
        String activityLevel,
        @NotBlank(message = "请选择目标")
        @Pattern(regexp = "MAINTAIN|FAT_LOSS|WEIGHT_GAIN|MUSCLE_GAIN", message = "目标选项不合法")
        String goal,
        @Size(max = 8, message = "饮食禁忌最多填写 8 项")
        List<@NotBlank @Size(max = 40) String> dietaryRestrictions,
        @Size(max = 8, message = "过敏信息最多填写 8 项")
        List<@NotBlank @Size(max = 40) String> allergies,
        @Size(max = 160, message = "特殊健康情况不能超过 160 个字")
        String specialHealthCondition,
        @NotBlank(message = "请选择营养目标状态")
        @Pattern(regexp = "DISABLED|AI_REFERENCE|CUSTOM", message = "营养目标状态不合法")
        String targetMode,
        @Valid NutritionValues customTarget
) {
}
