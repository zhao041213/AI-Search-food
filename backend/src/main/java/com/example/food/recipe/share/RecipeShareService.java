package com.example.food.recipe.share;

import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.example.food.recipe.RecipeRecord;
import com.example.food.recipe.RecipeRecordMapper;
import com.example.food.recipe.SavedRecipeService;
import com.example.food.recipe.dto.RecipeHistoryDetailResponse;
import com.example.food.recipe.share.dto.PublicRecipeResponse;
import com.example.food.recipe.share.dto.RecipeShareCreateRequest;
import com.example.food.recipe.share.dto.RecipeShareResponse;
import com.example.food.security.UserSecurityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;

@Service
public class RecipeShareService {

    public static final String ACTIVE = "ACTIVE";
    public static final String EXPIRED = "EXPIRED";
    public static final String DISABLED = "DISABLED";
    public static final String INVALID = "INVALID";
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RecipeShareMapper shareMapper;
    private final RecipeRecordMapper recipeRecordMapper;
    private final SavedRecipeService savedRecipeService;
    private final UserSecurityLogService securityLogService;
    private final Clock clock;

    @Autowired
    public RecipeShareService(
            RecipeShareMapper shareMapper,
            RecipeRecordMapper recipeRecordMapper,
            SavedRecipeService savedRecipeService,
            UserSecurityLogService securityLogService
    ) {
        this(
                shareMapper,
                recipeRecordMapper,
                savedRecipeService,
                securityLogService,
                Clock.system(ZoneId.of("Asia/Shanghai"))
        );
    }

    RecipeShareService(
            RecipeShareMapper shareMapper,
            RecipeRecordMapper recipeRecordMapper,
            SavedRecipeService savedRecipeService,
            UserSecurityLogService securityLogService,
            Clock clock
    ) {
        this.shareMapper = shareMapper;
        this.recipeRecordMapper = recipeRecordMapper;
        this.savedRecipeService = savedRecipeService;
        this.securityLogService = securityLogService;
        this.clock = clock;
    }

    @Transactional
    public RecipeShareResponse createShare(
            Long userId,
            Long recipeId,
            RecipeShareCreateRequest request
    ) {
        ownedRecipe(userId, recipeId);
        Validity validity = validityOf(request == null ? null : request.validity());
        RecipeShare share = new RecipeShare();
        share.setUserId(userId);
        share.setRecipeId(recipeId);
        share.setToken(newToken());
        share.setValidityType(validity.type());
        share.setExpiresAt(validity.expiresAt());
        share.setCreatedAt(now());
        try {
            shareMapper.insert(share);
        } catch (DuplicateKeyException exception) {
            share.setToken(newToken());
            shareMapper.insert(share);
        }
        securityLogService.record(userId, UserSecurityLogService.SHARE_CREATE,
                "/api/users/me/saved-recipes/" + recipeId + "/shares", "create recipe share");
        return responseOf(share, userId);
    }

    @Transactional(readOnly = true)
    public List<RecipeShareResponse> listShares(Long userId) {
        return shareMapper.findByUserId(userId).stream()
                .map(share -> responseOf(share, userId))
                .toList();
    }

    @Transactional
    public void disableShare(Long userId, Long shareId) {
        ownedShare(userId, shareId);
        if (shareMapper.disableOwned(userId, shareId, now()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "分享链接不存在");
        }
        securityLogService.record(userId, UserSecurityLogService.SHARE_DISABLE,
                "/api/users/me/recipe-shares/" + shareId + "/disable", "disable recipe share");
    }

    @Transactional(readOnly = true)
    public PublicRecipeResponse publicRecipe(String token) {
        if (!StringUtils.hasText(token) || token.trim().length() > 64) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "分享链接不存在或已失效");
        }
        RecipeShare share = shareMapper.findByToken(token.trim());
        if (share == null || share.getRecipeId() == null || share.getDisabledAt() != null
                || (share.getExpiresAt() != null && !share.getExpiresAt().isAfter(now()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "分享链接不存在或已失效");
        }
        RecipeRecord record = recipeRecordMapper.selectById(share.getRecipeId());
        if (record == null || !share.getUserId().equals(record.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "分享链接不存在或已失效");
        }
        RecipeHistoryDetailResponse detail = savedRecipeService.detail(share.getUserId(), record.getId());
        return publicResponse(detail.recipe());
    }

    private RecipeShareResponse responseOf(RecipeShare share, Long userId) {
        RecipeRecord record = share.getRecipeId() == null
                ? null
                : recipeRecordMapper.selectById(share.getRecipeId());
        String status = statusOf(share, record);
        String title = record != null && userId.equals(record.getUserId())
                ? record.getTitle()
                : "菜谱已取消收藏";
        return new RecipeShareResponse(
                share.getId(),
                share.getRecipeId(),
                title,
                share.getToken(),
                "/shared/recipes/" + share.getToken(),
                validityLabel(share.getValidityType()),
                share.getExpiresAt(),
                share.getDisabledAt(),
                status,
                share.getCreatedAt()
        );
    }

    private String statusOf(RecipeShare share, RecipeRecord record) {
        if (share == null || share.getRecipeId() == null
                || record == null || !share.getUserId().equals(record.getUserId())) {
            return INVALID;
        }
        if (share.getDisabledAt() != null) {
            return DISABLED;
        }
        if (share.getExpiresAt() != null && !share.getExpiresAt().isAfter(now())) {
            return EXPIRED;
        }
        return ACTIVE;
    }

    private RecipeShare ownedShare(Long userId, Long shareId) {
        RecipeShare share = shareMapper.findOwned(userId, shareId);
        if (share == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "分享链接不存在");
        }
        return share;
    }

    private RecipeRecord ownedRecipe(Long userId, Long recipeId) {
        RecipeRecord record = recipeId == null ? null : recipeRecordMapper.selectById(recipeId);
        if (record == null || !userId.equals(record.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "菜谱不存在");
        }
        return record;
    }

    private Validity validityOf(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("分享有效期不能为空");
        }
        LocalDateTime now = now();
        return switch (value.trim()) {
            case "1" -> new Validity("DAY_1", now.plusDays(1));
            case "7" -> new Validity("DAY_7", now.plusDays(7));
            case "30" -> new Validity("DAY_30", now.plusDays(30));
            case "PERMANENT" -> new Validity("PERMANENT", null);
            default -> throw new IllegalArgumentException("分享有效期不合法");
        };
    }

    private String validityLabel(String type) {
        return switch (type) {
            case "DAY_1" -> "1天";
            case "DAY_7" -> "7天";
            case "DAY_30" -> "30天";
            default -> "永久";
        };
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private PublicRecipeResponse publicResponse(RecipeGenerateResponse recipe) {
        if (recipe == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "分享菜谱不存在");
        }
        List<PublicRecipeResponse.Ingredient> ingredients = recipe.ingredients().stream()
                .map(item -> new PublicRecipeResponse.Ingredient(item.name(), item.amount()))
                .toList();
        List<PublicRecipeResponse.Step> steps = recipe.steps().stream()
                .map(item -> new PublicRecipeResponse.Step(
                        item.order(), item.title(), item.description(), item.durationMinutes()))
                .toList();
        RecipeGenerateResponse.NutritionEstimate nutrition = recipe.nutritionEstimate();
        PublicRecipeResponse.Nutrition publicNutrition = nutrition == null
                ? null
                : new PublicRecipeResponse.Nutrition(
                        nutrition.servings(),
                        nutrition.caloriesKcal(),
                        nutrition.proteinG(),
                        nutrition.fatG(),
                        nutrition.carbohydrateG());
        return new PublicRecipeResponse(
                recipe.title(),
                recipe.summary(),
                ingredients,
                steps,
                recipe.tips(),
                publicNutrition
        );
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private record Validity(String type, LocalDateTime expiresAt) {
    }
}
