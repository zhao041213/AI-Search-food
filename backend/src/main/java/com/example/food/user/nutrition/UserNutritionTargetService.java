package com.example.food.user.nutrition;

import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.user.nutrition.dto.NutritionTargetRequest;
import com.example.food.user.nutrition.dto.NutritionTargetResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class UserNutritionTargetService {

    public static final BigDecimal MAX_CALORIES_KCAL = new BigDecimal("10000");
    public static final BigDecimal MAX_MACRO_G = new BigDecimal("1000");

    private final UserNutritionTargetMapper mapper;

    public UserNutritionTargetService(UserNutritionTargetMapper mapper) {
        this.mapper = mapper;
    }

    public NutritionTargetResponse get(Long userId) {
        UserNutritionTarget target = mapper.selectById(userId);
        return target == null ? NutritionTargetResponse.empty() : toResponse(target);
    }

    public RecommendationContext getRecommendationContext(Long userId) {
        UserNutritionTarget target = mapper.selectById(userId);
        if (target == null || !isUsable(target)) {
            return null;
        }
        return new RecommendationContext(
                target.getCaloriesKcal(),
                target.getProteinG(),
                target.getFatG(),
                target.getCarbohydrateG()
        );
    }

    public RecommendationContext getRecommendationContext(AuthPrincipal principal) {
        if (principal == null || principal.role() != AppRole.USER) {
            return null;
        }
        return getRecommendationContext(principal.id());
    }

    @Transactional
    public NutritionTargetResponse save(Long userId, NutritionTargetRequest request) {
        validate(request);
        UserNutritionTarget target = mapper.selectById(userId);
        boolean exists = target != null;
        LocalDateTime now = LocalDateTime.now();
        if (!exists) {
            target = new UserNutritionTarget();
            target.setUserId(userId);
            target.setCreatedAt(now);
        }
        target.setEnabled(request.enabled());
        if (Boolean.TRUE.equals(request.enabled())) {
            target.setCaloriesKcal(request.caloriesKcal());
            target.setProteinG(request.proteinG());
            target.setFatG(request.fatG());
            target.setCarbohydrateG(request.carbohydrateG());
        } else {
            target.setCaloriesKcal(null);
            target.setProteinG(null);
            target.setFatG(null);
            target.setCarbohydrateG(null);
        }
        target.setUpdatedAt(now);

        if (exists) {
            mapper.updateById(target);
        } else {
            try {
                mapper.insert(target);
            } catch (DuplicateKeyException exception) {
                target.setCreatedAt(null);
                mapper.updateById(target);
            }
        }
        return toResponse(target);
    }

    @Transactional
    public void delete(Long userId) {
        mapper.deleteById(userId);
    }

    private void validate(NutritionTargetRequest request) {
        if (request == null || request.enabled() == null) {
            throw new IllegalArgumentException("请选择是否启用每日营养目标");
        }
        if (!Boolean.TRUE.equals(request.enabled())) {
            return;
        }
        validateValue(request.caloriesKcal(), "每日热量", MAX_CALORIES_KCAL);
        validateValue(request.proteinG(), "每日蛋白质", MAX_MACRO_G);
        validateValue(request.fatG(), "每日脂肪", MAX_MACRO_G);
        validateValue(request.carbohydrateG(), "每日碳水", MAX_MACRO_G);
    }

    private void validateValue(BigDecimal value, String label, BigDecimal max) {
        if (value == null) {
            throw new IllegalArgumentException("请输入" + label + "目标");
        }
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(label + "目标必须大于 0");
        }
        if (value.compareTo(max) > 0) {
            throw new IllegalArgumentException(label + "目标不能超过 " + max.stripTrailingZeros().toPlainString());
        }
        if (value.scale() > 2) {
            throw new IllegalArgumentException(label + "目标最多保留 2 位小数");
        }
    }

    private boolean isUsable(UserNutritionTarget target) {
        return Boolean.TRUE.equals(target.getEnabled())
                && validValue(target.getCaloriesKcal(), MAX_CALORIES_KCAL)
                && validValue(target.getProteinG(), MAX_MACRO_G)
                && validValue(target.getFatG(), MAX_MACRO_G)
                && validValue(target.getCarbohydrateG(), MAX_MACRO_G);
    }

    private boolean validValue(BigDecimal value, BigDecimal max) {
        return value != null && value.signum() > 0 && value.scale() <= 2 && value.compareTo(max) <= 0;
    }

    private NutritionTargetResponse toResponse(UserNutritionTarget target) {
        boolean enabled = Boolean.TRUE.equals(target.getEnabled());
        return new NutritionTargetResponse(
                true,
                enabled,
                enabled ? target.getCaloriesKcal() : null,
                enabled ? target.getProteinG() : null,
                enabled ? target.getFatG() : null,
                enabled ? target.getCarbohydrateG() : null,
                target.getUpdatedAt()
        );
    }

    public record RecommendationContext(
            BigDecimal caloriesKcal,
            BigDecimal proteinG,
            BigDecimal fatG,
            BigDecimal carbohydrateG
    ) {
    }
}
