package com.example.food.agent;

import com.example.food.notification.NotificationService;
import com.example.food.notification.dto.NotificationPreferenceRequest;
import com.example.food.pantry.PantryOperationService;
import com.example.food.pantry.UserPantryService;
import com.example.food.pantry.dto.CookingConsumptionRequest;
import com.example.food.pantry.dto.PantryItemRequest;
import com.example.food.recipe.RecommendationFeedbackService;
import com.example.food.recipe.SavedRecipeService;
import com.example.food.recipe.collection.SavedRecipeCollectionService;
import com.example.food.recipe.collection.dto.RecipeCollectionRequest;
import com.example.food.recipe.collection.dto.SavedRecipeBatchDeleteRequest;
import com.example.food.recipe.collection.dto.SavedRecipeBatchMoveRequest;
import com.example.food.recipe.collection.dto.SavedRecipeBatchTagsRequest;
import com.example.food.recipe.collection.dto.SavedRecipeCollectionRequest;
import com.example.food.recipe.collection.dto.SavedRecipeTagsRequest;
import com.example.food.recipe.dto.RecommendationReactionRequest;
import com.example.food.recipe.share.RecipeShareService;
import com.example.food.recipe.share.dto.RecipeShareCreateRequest;
import com.example.food.review.FinishedDishReviewService;
import com.example.food.security.AuthPrincipal;
import com.example.food.shopping.ShoppingItemCheckService;
import com.example.food.shopping.dto.ShoppingItemCheckRequest;
import com.example.food.user.character.UserKitchenCharacterNamesService;
import com.example.food.user.character.dto.KitchenCharacterNamesRequest;
import com.example.food.user.health.UserHealthProfileService;
import com.example.food.user.health.dto.HealthProfileRequest;
import com.example.food.user.nutrition.UserNutritionTargetService;
import com.example.food.user.nutrition.dto.NutritionTargetRequest;
import com.example.food.user.preference.UserDietPreferenceService;
import com.example.food.user.preference.dto.DietPreferenceRequest;
import com.example.food.weekly.WeeklyMenuService;
import com.example.food.weekly.dto.WeeklyMenuAutoGenerateRequest;
import com.example.food.weekly.dto.WeeklyMenuSaveRequest;
import com.example.food.weekly.dto.WeeklyMenuShoppingStatusRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Service
public class AgentKitchenActionService {

    private final UserPantryService pantryService;
    private final PantryOperationService pantryOperationService;
    private final WeeklyMenuService weeklyMenuService;
    private final ShoppingItemCheckService shoppingItemCheckService;
    private final NotificationService notificationService;
    private final SavedRecipeService savedRecipeService;
    private final SavedRecipeCollectionService collectionService;
    private final RecipeShareService shareService;
    private final RecommendationFeedbackService feedbackService;
    private final UserHealthProfileService healthProfileService;
    private final UserDietPreferenceService dietPreferenceService;
    private final UserNutritionTargetService nutritionTargetService;
    private final UserKitchenCharacterNamesService characterNamesService;
    private final FinishedDishReviewService finishedDishReviewService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public AgentKitchenActionService(
            UserPantryService pantryService,
            PantryOperationService pantryOperationService,
            WeeklyMenuService weeklyMenuService,
            ShoppingItemCheckService shoppingItemCheckService,
            NotificationService notificationService,
            SavedRecipeService savedRecipeService,
            SavedRecipeCollectionService collectionService,
            RecipeShareService shareService,
            RecommendationFeedbackService feedbackService,
            UserHealthProfileService healthProfileService,
            UserDietPreferenceService dietPreferenceService,
            UserNutritionTargetService nutritionTargetService,
            UserKitchenCharacterNamesService characterNamesService,
            FinishedDishReviewService finishedDishReviewService,
            ObjectMapper objectMapper,
            Validator validator
    ) {
        this.pantryService = pantryService;
        this.pantryOperationService = pantryOperationService;
        this.weeklyMenuService = weeklyMenuService;
        this.shoppingItemCheckService = shoppingItemCheckService;
        this.notificationService = notificationService;
        this.savedRecipeService = savedRecipeService;
        this.collectionService = collectionService;
        this.shareService = shareService;
        this.feedbackService = feedbackService;
        this.healthProfileService = healthProfileService;
        this.dietPreferenceService = dietPreferenceService;
        this.nutritionTargetService = nutritionTargetService;
        this.characterNamesService = characterNamesService;
        this.finishedDishReviewService = finishedDishReviewService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public ActionResult execute(
            String actionType,
            JsonNode payload,
            AuthPrincipal principal,
            String idempotencyKey
    ) {
        Long userId = principal.id();
        return switch (actionType) {
            case "PANTRY_CREATE" -> result("食材已加入库存", pantryService.create(userId, request(payload, PantryItemRequest.class)));
            case "PANTRY_UPDATE" -> result("食材库存已更新", pantryService.update(userId, id(payload), request(payload, PantryItemRequest.class)));
            case "PANTRY_CONSUME" -> result("食材消耗已记录", pantryService.consume(userId, id(payload), decimal(payload, "quantity")));
            case "PANTRY_DELETE" -> {
                pantryService.delete(userId, id(payload));
                yield result("食材已从库存删除", null);
            }
            case "PANTRY_UNDO" -> result("库存操作已撤销", pantryOperationService.undo(userId, id(payload), idempotencyKey));
            case "COOKING_CONSUME" -> {
                CookingConsumptionRequest source = request(payload, CookingConsumptionRequest.class);
                CookingConsumptionRequest safeRequest = new CookingConsumptionRequest(
                        source.recipeId(), source.actualServings(), idempotencyKey, source.items());
                yield result("本次做菜消耗已扣减", pantryOperationService.consume(userId, safeRequest));
            }
            case "WEEKLY_MENU_GENERATE" -> result("周菜单已生成", weeklyMenuService.autoGenerate(userId, request(payload, WeeklyMenuAutoGenerateRequest.class)));
            case "WEEKLY_MENU_SAVE" -> result("周菜单已保存", weeklyMenuService.save(userId, request(payload, WeeklyMenuSaveRequest.class)));
            case "WEEKLY_MENU_CLEAR" -> {
                weeklyMenuService.delete(userId, date(payload, "weekStart"));
                yield result("周菜单已清空", null);
            }
            case "WEEKLY_SHOPPING_UPDATE" -> result("购物状态已更新", weeklyMenuService.saveShoppingStatus(userId, request(payload, WeeklyMenuShoppingStatusRequest.class)));
            case "RECIPE_SHOPPING_UPDATE" -> result("购物项状态已更新", shoppingItemCheckService.save(userId, request(payload, ShoppingItemCheckRequest.class)));
            case "NOTIFICATION_READ" -> result("提醒已标记为已读", notificationService.markRead(userId, id(payload)));
            case "NOTIFICATION_READ_ALL" -> result("全部提醒已标记为已读", notificationService.markAllRead(userId));
            case "NOTIFICATION_ARCHIVE" -> result("提醒已归档", notificationService.archive(userId, id(payload)));
            case "NOTIFICATION_PREFERENCES_UPDATE" -> result("提醒偏好已更新", notificationService.updatePreferences(userId, request(payload, NotificationPreferenceRequest.class)));
            case "RECIPE_DELETE" -> {
                savedRecipeService.delete(userId, id(payload));
                yield result("菜谱已删除", null);
            }
            case "COLLECTION_CREATE" -> result("收藏夹已创建", collectionService.createCollection(userId, request(payload, RecipeCollectionRequest.class)));
            case "COLLECTION_RENAME" -> result("收藏夹已重命名", collectionService.renameCollection(userId, longValue(payload, "collectionId"), request(payload, RecipeCollectionRequest.class)));
            case "COLLECTION_DELETE" -> {
                collectionService.deleteCollection(userId, longValue(payload, "collectionId"), true);
                yield result("收藏夹已删除", null);
            }
            case "RECIPE_MOVE" -> {
                collectionService.moveRecipe(userId, longValue(payload, "recipeId"), request(payload, SavedRecipeCollectionRequest.class));
                yield result("菜谱已移动到指定收藏夹", null);
            }
            case "RECIPE_TAGS_REPLACE" -> {
                collectionService.replaceTags(userId, longValue(payload, "recipeId"), request(payload, SavedRecipeTagsRequest.class));
                yield result("菜谱标签已更新", null);
            }
            case "RECIPE_BATCH_MOVE" -> result("菜谱批量移动完成", collectionService.batchMove(userId, request(payload, SavedRecipeBatchMoveRequest.class)));
            case "RECIPE_BATCH_TAGS" -> result("菜谱批量标签操作完成", collectionService.batchTags(userId, request(payload, SavedRecipeBatchTagsRequest.class)));
            case "RECIPE_BATCH_DELETE" -> result("菜谱批量删除完成", collectionService.batchDelete(userId, request(payload, SavedRecipeBatchDeleteRequest.class)));
            case "RECIPE_SHARE_CREATE" -> result("菜谱分享链接已创建", shareService.createShare(userId, longValue(payload, "recipeId"), request(payload, RecipeShareCreateRequest.class)));
            case "RECIPE_SHARE_DISABLE" -> {
                shareService.disableShare(userId, longValue(payload, "shareId"));
                yield result("菜谱分享链接已停用", null);
            }
            case "RECOMMENDATION_REACTION_SET" -> result("推荐反馈已记录", feedbackService.setReaction(longValue(payload, "searchLogId"), request(payload, RecommendationReactionRequest.class), principal, null));
            case "RECOMMENDATION_REACTION_CLEAR" -> result("推荐反馈已清除", feedbackService.clearReaction(longValue(payload, "searchLogId"), principal, null));
            case "RECOMMENDATION_MARK_COOKED" -> result("已记录为做过", feedbackService.markCooked(longValue(payload, "searchLogId"), principal, null));
            case "HEALTH_PROFILE_UPDATE" -> result("健康档案已更新", healthProfileService.save(userId, request(payload, HealthProfileRequest.class)));
            case "HEALTH_PROFILE_DELETE" -> {
                healthProfileService.delete(userId);
                yield result("健康档案已删除", null);
            }
            case "DIET_PREFERENCE_UPDATE" -> result("饮食偏好已更新", dietPreferenceService.save(userId, request(payload, DietPreferenceRequest.class)));
            case "NUTRITION_TARGET_UPDATE" -> result("营养目标已更新", nutritionTargetService.save(userId, request(payload, NutritionTargetRequest.class)));
            case "NUTRITION_TARGET_DELETE" -> {
                nutritionTargetService.delete(userId);
                yield result("营养目标已删除", null);
            }
            case "CHARACTER_NAMES_UPDATE" -> result("厨房角色名称已更新", characterNamesService.save(userId, request(payload, KitchenCharacterNamesRequest.class)));
            case "CHARACTER_NAMES_RESET" -> result("厨房角色名称已恢复默认", characterNamesService.reset(userId));
            case "FINISHED_DISH_REVIEW_DELETE" -> {
                finishedDishReviewService.delete(userId, id(payload));
                yield result("成品评价已删除", null);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的确认操作");
        };
    }

    private ActionResult result(String message, Object detail) {
        return new ActionResult(message, detail);
    }

    private <T> T request(JsonNode payload, Class<T> type) {
        JsonNode node = payload == null ? null : payload.path("request");
        if (node == null || node.isMissingNode() || !node.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "操作参数 request 缺失");
        }
        try {
            T value = objectMapper.treeToValue(node, type);
            Set<ConstraintViolation<T>> violations = validator.validate(value);
            if (!violations.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, violations.iterator().next().getMessage());
            }
            return value;
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "操作参数格式不正确", exception);
        }
    }

    private Long id(JsonNode payload) {
        return longValue(payload, "id");
    }

    private Long longValue(JsonNode payload, String field) {
        JsonNode value = payload == null ? null : payload.path(field);
        if (value == null || !value.canConvertToLong() || value.longValue() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " 必须是有效编号");
        }
        return value.longValue();
    }

    private BigDecimal decimal(JsonNode payload, String field) {
        JsonNode value = payload == null ? null : payload.path(field);
        if (value == null || !value.isNumber() || value.decimalValue().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " 必须大于 0");
        }
        return value.decimalValue();
    }

    private LocalDate date(JsonNode payload, String field) {
        String value = payload == null ? "" : payload.path(field).asText("");
        try {
            return value.isBlank() ? LocalDate.now() : LocalDate.parse(value);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " 必须使用 yyyy-MM-dd 格式");
        }
    }

    public record ActionResult(String message, Object detail) {
    }
}
