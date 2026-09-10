package com.example.food.user.healthnutrition;

import com.example.food.user.healthnutrition.dto.HealthNutritionProfileRequest;
import com.example.food.user.healthnutrition.dto.HealthNutritionProfileResponse;
import com.example.food.user.healthnutrition.dto.NutritionValues;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class HealthNutritionService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };
    private final HealthNutritionProfileMapper mapper;
    private final ObjectMapper objectMapper;

    public HealthNutritionService(HealthNutritionProfileMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public HealthNutritionProfileResponse get(Long userId) {
        HealthNutritionProfile profile = mapper.findByUserId(userId);
        return profile == null ? HealthNutritionProfileResponse.empty() : toResponse(profile);
    }

    @Transactional
    public HealthNutritionProfileResponse save(Long userId, HealthNutritionProfileRequest request) {
        validateTarget(request);
        LocalDateTime now = LocalDateTime.now();
        HealthNutritionProfile profile = mapper.findByUserId(userId);
        if (profile == null) {
            profile = new HealthNutritionProfile();
            profile.setUserId(userId);
            profile.setCreatedAt(now);
            apply(profile, request, now);
            try {
                mapper.insert(profile);
            } catch (DuplicateKeyException exception) {
                profile = mapper.findByUserId(userId);
                if (profile == null) throw exception;
                apply(profile, request, now);
                mapper.updateByUserId(profile);
            }
        } else {
            apply(profile, request, now);
            mapper.updateByUserId(profile);
        }
        return toResponse(profile);
    }

    @Transactional
    public HealthNutritionProfileResponse generateAiReference(Long userId) {
        HealthNutritionProfile profile = mapper.findByUserId(userId);
        if (profile == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先保存健康与营养信息");
        }
        NutritionValues values = calculateReference(profile);
        profile.setAiCaloriesKcal(values.caloriesKcal());
        profile.setAiProteinG(values.proteinG());
        profile.setAiFatG(values.fatG());
        profile.setAiCarbohydrateG(values.carbohydrateG());
        profile.setAiReferenceBasis("基于性别、年龄、身高、体重、活动强度和目标的通用热量估算，仅作一般饮食参考");
        profile.setAiReferenceUpdatedAt(LocalDateTime.now());
        if ("AI_REFERENCE".equals(profile.getTargetMode())) {
            profile.setCurrentCaloriesKcal(values.caloriesKcal());
            profile.setCurrentProteinG(values.proteinG());
            profile.setCurrentFatG(values.fatG());
            profile.setCurrentCarbohydrateG(values.carbohydrateG());
        }
        profile.setUpdatedAt(LocalDateTime.now());
        mapper.updateByUserId(profile);
        return toResponse(profile);
    }

    public RecommendationContext recommendationContext(Long userId) {
        HealthNutritionProfile profile = mapper.findByUserId(userId);
        if (profile == null) return null;
        NutritionValues target = currentTarget(profile);
        return new RecommendationContext(profile.getGender(), profile.getAge(), profile.getHeightCm(),
                profile.getWeightKg(), profile.getActivityLevel(), profile.getGoal(),
                readList(profile.getDietaryRestrictionsJson()), readList(profile.getAllergiesJson()),
                profile.getSpecialHealthCondition(), target);
    }

    private void apply(HealthNutritionProfile profile, HealthNutritionProfileRequest request, LocalDateTime now) {
        profile.setGender(normalize(request.gender()));
        profile.setAge(request.age());
        profile.setHeightCm(request.heightCm());
        profile.setWeightKg(request.weightKg());
        profile.setActivityLevel(normalize(request.activityLevel()));
        profile.setGoal(normalize(request.goal()));
        profile.setDietaryRestrictionsJson(toJson(normalizeList(request.dietaryRestrictions())));
        profile.setAllergiesJson(toJson(normalizeList(request.allergies())));
        profile.setSpecialHealthCondition(limit(request.specialHealthCondition(), 160));
        profile.setTargetMode(normalize(request.targetMode()));
        NutritionValues custom = request.customTarget();
        if ("CUSTOM".equals(profile.getTargetMode())) {
            if (custom == null || !validValues(custom)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请完整填写合理的自定义营养目标");
            }
            profile.setCurrentCaloriesKcal(custom.caloriesKcal());
            profile.setCurrentProteinG(custom.proteinG());
            profile.setCurrentFatG(custom.fatG());
            profile.setCurrentCarbohydrateG(custom.carbohydrateG());
        } else if ("AI_REFERENCE".equals(profile.getTargetMode())) {
            if (profile.getAiCaloriesKcal() == null || profile.getAiProteinG() == null
                    || profile.getAiFatG() == null || profile.getAiCarbohydrateG() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先生成 AI 参考值");
            }
            profile.setCurrentCaloriesKcal(profile.getAiCaloriesKcal());
            profile.setCurrentProteinG(profile.getAiProteinG());
            profile.setCurrentFatG(profile.getAiFatG());
            profile.setCurrentCarbohydrateG(profile.getAiCarbohydrateG());
        } else {
            profile.setCurrentCaloriesKcal(null);
            profile.setCurrentProteinG(null);
            profile.setCurrentFatG(null);
            profile.setCurrentCarbohydrateG(null);
        }
        profile.setUpdatedAt(now);
    }

    private NutritionValues calculateReference(HealthNutritionProfile profile) {
        BigDecimal bmr = profile.getWeightKg().multiply(BigDecimal.valueOf(10))
                .add(profile.getHeightCm().multiply(BigDecimal.valueOf(6.25)))
                .subtract(BigDecimal.valueOf(profile.getAge()).multiply(BigDecimal.valueOf(5)))
                .add(switch (profile.getGender()) {
                    case "FEMALE" -> BigDecimal.valueOf(-161);
                    case "MALE" -> BigDecimal.valueOf(5);
                    default -> BigDecimal.valueOf(-78);
                });
        BigDecimal activityFactor = switch (profile.getActivityLevel()) {
            case "LOW" -> BigDecimal.valueOf(1.2);
            case "HIGH" -> BigDecimal.valueOf(1.725);
            default -> BigDecimal.valueOf(1.55);
        };
        BigDecimal calories = bmr.multiply(activityFactor);
        calories = switch (profile.getGoal()) {
            case "FAT_LOSS" -> calories.subtract(BigDecimal.valueOf(300));
            case "WEIGHT_GAIN" -> calories.add(BigDecimal.valueOf(250));
            case "MUSCLE_GAIN" -> calories.add(BigDecimal.valueOf(200));
            default -> calories;
        };
        calories = calories.max(BigDecimal.valueOf(1200)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal protein = profile.getWeightKg().multiply("MUSCLE_GAIN".equals(profile.getGoal())
                ? BigDecimal.valueOf(1.6) : BigDecimal.valueOf(1.2)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal fat = calories.multiply(BigDecimal.valueOf(0.27)).divide(BigDecimal.valueOf(9), 0, RoundingMode.HALF_UP);
        BigDecimal carbohydrate = calories.subtract(protein.multiply(BigDecimal.valueOf(4)))
                .subtract(fat.multiply(BigDecimal.valueOf(9))).max(BigDecimal.ZERO)
                .divide(BigDecimal.valueOf(4), 0, RoundingMode.HALF_UP);
        return new NutritionValues(calories, protein, fat, carbohydrate);
    }

    private void validateTarget(HealthNutritionProfileRequest request) {
        if ("CUSTOM".equals(request.targetMode()) && !validValues(request.customTarget())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "自定义营养目标必须全部填写且在合理范围内");
        }
    }

    private boolean validValues(NutritionValues values) {
        return values != null && between(values.caloriesKcal(), 800, 10000)
                && between(values.proteinG(), 1, 1000) && between(values.fatG(), 1, 1000)
                && between(values.carbohydrateG(), 1, 1000);
    }

    private boolean between(BigDecimal value, int min, int max) {
        return value != null && value.compareTo(BigDecimal.valueOf(min)) >= 0 && value.compareTo(BigDecimal.valueOf(max)) <= 0;
    }

    private HealthNutritionProfileResponse toResponse(HealthNutritionProfile profile) {
        return new HealthNutritionProfileResponse(
                true, profile.getGender(), profile.getAge(), profile.getHeightCm(), profile.getWeightKg(), bmi(profile),
                profile.getActivityLevel(), profile.getGoal(), readList(profile.getDietaryRestrictionsJson()),
                readList(profile.getAllergiesJson()), profile.getSpecialHealthCondition(), profile.getTargetMode(),
                values(profile.getAiCaloriesKcal(), profile.getAiProteinG(), profile.getAiFatG(), profile.getAiCarbohydrateG()),
                currentTarget(profile), profile.getAiReferenceBasis(), profile.getAiReferenceUpdatedAt(), profile.getUpdatedAt(),
                profile.getAge() < 18 || profile.getAge() >= 65 || StringUtils.hasText(profile.getSpecialHealthCondition())
        );
    }

    private NutritionValues currentTarget(HealthNutritionProfile profile) {
        if ("DISABLED".equals(profile.getTargetMode())) return null;
        return values(profile.getCurrentCaloriesKcal(), profile.getCurrentProteinG(), profile.getCurrentFatG(), profile.getCurrentCarbohydrateG());
    }

    private NutritionValues values(BigDecimal calories, BigDecimal protein, BigDecimal fat, BigDecimal carbs) {
        return calories == null || protein == null || fat == null || carbs == null ? null : new NutritionValues(calories, protein, fat, carbs);
    }

    private BigDecimal bmi(HealthNutritionProfile profile) {
        return profile.getWeightKg().divide(profile.getHeightCm().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP).pow(2), 1, RoundingMode.HALF_UP);
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) return List.of();
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            String item = limit(value, 40);
            if (StringUtils.hasText(item) && !normalized.contains(item)) normalized.add(item);
        }
        return List.copyOf(normalized);
    }

    private List<String> readList(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        try { return List.copyOf(objectMapper.readValue(value, STRING_LIST)); }
        catch (JsonProcessingException exception) { return List.of(); }
    }

    private String toJson(List<String> values) {
        try { return objectMapper.writeValueAsString(values); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("健康数据保存失败", exception); }
    }

    private String normalize(String value) { return value == null ? null : value.trim().toUpperCase(Locale.ROOT); }
    private String limit(String value, int max) { if (!StringUtils.hasText(value)) return null; String trimmed = value.trim(); return trimmed.substring(0, Math.min(trimmed.length(), max)); }

    public record RecommendationContext(
            String gender, Integer age, BigDecimal heightCm, BigDecimal weightKg, String activityLevel, String goal,
            List<String> dietaryRestrictions, List<String> allergies, String specialHealthCondition, NutritionValues target
    ) { }
}
