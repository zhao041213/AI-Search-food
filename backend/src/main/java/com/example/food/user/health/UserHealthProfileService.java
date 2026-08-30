package com.example.food.user.health;

import com.example.food.user.health.dto.HealthProfileRequest;
import com.example.food.user.health.dto.HealthProfileResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class UserHealthProfileService {

    private static final BigDecimal BMI_MULTIPLIER = BigDecimal.valueOf(10_000);

    private final UserHealthProfileMapper mapper;

    public UserHealthProfileService(UserHealthProfileMapper mapper) {
        this.mapper = mapper;
    }

    public HealthProfileResponse get(Long userId) {
        UserHealthProfile profile = mapper.selectById(userId);
        return profile == null ? HealthProfileResponse.empty() : toResponse(profile);
    }

    public RecommendationContext getRecommendationContext(Long userId) {
        UserHealthProfile profile = mapper.selectById(userId);
        if (profile == null) {
            return null;
        }
        return new RecommendationContext(
                profile.getAgeRange(),
                profile.getHeightCm(),
                profile.getWeightKg(),
                profile.getActivityLevel(),
                calculateBmi(profile.getHeightCm(), profile.getWeightKg())
        );
    }

    @Transactional
    public HealthProfileResponse save(Long userId, HealthProfileRequest request) {
        UserHealthProfile profile = mapper.selectById(userId);
        boolean exists = profile != null;
        LocalDateTime now = LocalDateTime.now();
        if (!exists) {
            profile = new UserHealthProfile();
            profile.setUserId(userId);
            profile.setCreatedAt(now);
        }
        profile.setAgeRange(request.ageRange());
        profile.setHeightCm(request.heightCm());
        profile.setWeightKg(request.weightKg());
        profile.setActivityLevel(request.activityLevel());
        profile.setUpdatedAt(now);

        if (exists) {
            mapper.updateById(profile);
        } else {
            try {
                mapper.insert(profile);
            } catch (DuplicateKeyException exception) {
                profile.setCreatedAt(null);
                mapper.updateById(profile);
            }
        }
        return toResponse(profile);
    }

    @Transactional
    public void delete(Long userId) {
        mapper.deleteById(userId);
    }

    private HealthProfileResponse toResponse(UserHealthProfile profile) {
        return new HealthProfileResponse(
                true,
                profile.getAgeRange(),
                profile.getHeightCm(),
                profile.getWeightKg(),
                profile.getActivityLevel(),
                calculateBmi(profile.getHeightCm(), profile.getWeightKg()),
                profile.getUpdatedAt()
        );
    }

    private BigDecimal calculateBmi(BigDecimal heightCm, BigDecimal weightKg) {
        return weightKg.multiply(BMI_MULTIPLIER)
                .divide(heightCm.multiply(heightCm), 1, RoundingMode.HALF_UP);
    }

    public record RecommendationContext(
            String ageRange,
            BigDecimal heightCm,
            BigDecimal weightKg,
            String activityLevel,
            BigDecimal bmi
    ) {
    }
}
