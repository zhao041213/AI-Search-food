package com.example.food.agent;

import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.example.food.recipe.SavedRecipeService;
import com.example.food.recipe.dto.RecipeHistoryDetailResponse;
import com.example.food.recipe.dto.SaveRecipeRequest;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.UserSecurityLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class AgentWriteService {

    private static final String ACTION_SAVE_RECIPE = "SAVE_RECIPE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_CONFIRMED = "CONFIRMED";

    private final AgentConfirmationMapper confirmationMapper;
    private final SavedRecipeService savedRecipeService;
    private final AgentKitchenActionService actionService;
    private final UserSecurityLogService securityLogService;
    private final ObjectMapper objectMapper;

    public AgentWriteService(
            AgentConfirmationMapper confirmationMapper,
            SavedRecipeService savedRecipeService,
            AgentKitchenActionService actionService,
            UserSecurityLogService securityLogService,
            ObjectMapper objectMapper
    ) {
        this.confirmationMapper = confirmationMapper;
        this.savedRecipeService = savedRecipeService;
        this.actionService = actionService;
        this.securityLogService = securityLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ConfirmationResult execute(AuthPrincipal principal, Long confirmationId, String idempotencyKey) {
        AgentConfirmation confirmation = ownedConfirmation(principal, confirmationId, idempotencyKey);
        if (STATUS_CONFIRMED.equals(confirmation.getStatus())) {
            return ConfirmationResult.alreadyCompleted();
        }
        if (STATUS_PROCESSING.equals(confirmation.getStatus())) {
            return ConfirmationResult.processing();
        }
        if (!STATUS_PENDING.equals(confirmation.getStatus())
                || confirmationMapper.claim(principal.id(), confirmationId) != 1) {
            return ConfirmationResult.processing();
        }

        ConfirmationResult result;
        if (ACTION_SAVE_RECIPE.equals(confirmation.getActionType())) {
            RecipeHistoryDetailResponse detail = saveRecipePayload(principal, confirmation.getPayloadJson());
            result = ConfirmationResult.completed(detail, "菜谱已保存到我的菜谱");
        } else {
            AgentKitchenActionService.ActionResult action = actionService.execute(
                    confirmation.getActionType(), readPayload(confirmation.getPayloadJson()), principal, idempotencyKey);
            result = ConfirmationResult.completed(action.detail(), action.message());
        }

        confirmationMapper.markConfirmed(principal.id(), confirmationId);
        securityLogService.record(
                principal.id(),
                "AGENT_" + confirmation.getActionType(),
                "/api/agent/chat/stream",
                "confirmationId=" + confirmationId
        );
        return result;
    }

    public ConfirmationResult saveRecipe(AuthPrincipal principal, Long confirmationId, String idempotencyKey) {
        AgentConfirmation confirmation = confirmationMapper.findOwned(principal.id(), confirmationId);
        if (confirmation == null || !ACTION_SAVE_RECIPE.equals(confirmation.getActionType())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "保存确认已失效");
        }
        return execute(principal, confirmationId, idempotencyKey);
    }

    private AgentConfirmation ownedConfirmation(AuthPrincipal principal, Long confirmationId, String idempotencyKey) {
        AgentConfirmation confirmation = confirmationMapper.findOwned(principal.id(), confirmationId);
        if (confirmation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "操作确认已失效");
        }
        if (!confirmation.getIdempotencyKey().equals(idempotencyKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "确认凭证不匹配，请重新发起操作");
        }
        if (confirmation.getCreatedAt() != null
                && confirmation.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(30))) {
            throw new ResponseStatusException(HttpStatus.GONE, "操作确认已过期，请重新发起");
        }
        return confirmation;
    }

    private RecipeHistoryDetailResponse saveRecipePayload(AuthPrincipal principal, String payloadJson) {
        RecipeGenerateResponse recipe = readRecipe(payloadJson);
        if (recipe.searchLogId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "本次菜谱缺少搜索记录，暂时无法安全保存");
        }
        SaveRecipeRequest request = new SaveRecipeRequest(
                recipe.searchLogId(), recipe.title(), recipe.summary(), recipe.effects(), recipe.ingredients(),
                recipe.missingIngredients(), recipe.steps(), recipe.tips(), recipe.videoKeywords(), recipe.explanation(),
                recipe.nutritionEstimate(), valueOrDefault(recipe.provider(), "qwen"), valueOrDefault(recipe.model(), "unknown")
        );
        return savedRecipeService.save(request, principal, null);
    }

    private RecipeGenerateResponse readRecipe(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, RecipeGenerateResponse.class);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "保存内容已损坏，请重新生成菜谱", exception);
        }
    }

    private JsonNode readPayload(String payloadJson) {
        try {
            JsonNode payload = objectMapper.readTree(payloadJson);
            if (payload == null || !payload.isObject()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "确认操作参数已损坏");
            }
            return payload;
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "确认操作参数已损坏", exception);
        }
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public record ConfirmationResult(String status, Object detail, String message) {
        static ConfirmationResult completed(Object detail, String message) {
            return new ConfirmationResult("completed", detail, message);
        }

        static ConfirmationResult alreadyCompleted() {
            return new ConfirmationResult("already-completed", null, "这项操作已经执行过了，不会重复处理");
        }

        static ConfirmationResult processing() {
            return new ConfirmationResult("processing", null, "操作正在处理中，请稍后查看结果");
        }
    }
}
