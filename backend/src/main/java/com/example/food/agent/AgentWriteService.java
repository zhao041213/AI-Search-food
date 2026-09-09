package com.example.food.agent;

import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.example.food.recipe.SavedRecipeService;
import com.example.food.recipe.dto.RecipeHistoryDetailResponse;
import com.example.food.recipe.dto.SaveRecipeRequest;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.UserSecurityLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    private final UserSecurityLogService securityLogService;
    private final ObjectMapper objectMapper;

    public AgentWriteService(
            AgentConfirmationMapper confirmationMapper,
            SavedRecipeService savedRecipeService,
            UserSecurityLogService securityLogService,
            ObjectMapper objectMapper
    ) {
        this.confirmationMapper = confirmationMapper;
        this.savedRecipeService = savedRecipeService;
        this.securityLogService = securityLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ConfirmationResult saveRecipe(
            AuthPrincipal principal,
            Long confirmationId,
            String idempotencyKey
    ) {
        AgentConfirmation confirmation = confirmationMapper.findOwned(principal.id(), confirmationId);
        if (confirmation == null || !ACTION_SAVE_RECIPE.equals(confirmation.getActionType())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "保存确认已失效");
        }
        if (!confirmation.getIdempotencyKey().equals(idempotencyKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "保存确认凭证不匹配，请重新发起保存");
        }
        if (confirmation.getCreatedAt() != null
                && confirmation.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(30))) {
            throw new ResponseStatusException(HttpStatus.GONE, "保存确认已过期，请重新发起保存");
        }
        if (STATUS_CONFIRMED.equals(confirmation.getStatus())) {
            return ConfirmationResult.alreadySaved();
        }
        if (STATUS_PROCESSING.equals(confirmation.getStatus())) {
            return ConfirmationResult.processing();
        }
        if (!STATUS_PENDING.equals(confirmation.getStatus())
                || confirmationMapper.claim(principal.id(), confirmationId) != 1) {
            return ConfirmationResult.processing();
        }

        RecipeGenerateResponse recipe = readRecipe(confirmation.getPayloadJson());
        if (recipe.searchLogId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "本次菜谱缺少搜索记录，暂时无法安全保存");
        }
        SaveRecipeRequest request = new SaveRecipeRequest(
                recipe.searchLogId(),
                recipe.title(),
                recipe.summary(),
                recipe.effects(),
                recipe.ingredients(),
                recipe.missingIngredients(),
                recipe.steps(),
                recipe.tips(),
                recipe.videoKeywords(),
                recipe.explanation(),
                recipe.nutritionEstimate(),
                valueOrDefault(recipe.provider(), "qwen"),
                valueOrDefault(recipe.model(), "unknown")
        );
        RecipeHistoryDetailResponse detail = savedRecipeService.save(request, principal, null);
        confirmationMapper.markConfirmed(principal.id(), confirmationId);
        securityLogService.record(
                principal.id(),
                "AGENT_SAVE_RECIPE",
                "/api/agent/chat/stream",
                "confirmationId=" + confirmationId + ", recipeId=" + detail.id()
        );
        return ConfirmationResult.saved(detail);
    }

    private RecipeGenerateResponse readRecipe(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, RecipeGenerateResponse.class);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "保存内容已损坏，请重新生成菜谱", exception);
        }
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public record ConfirmationResult(
            String status,
            RecipeHistoryDetailResponse detail,
            String message
    ) {
        static ConfirmationResult saved(RecipeHistoryDetailResponse detail) {
            return new ConfirmationResult("saved", detail, "菜谱已保存到我的菜谱");
        }

        static ConfirmationResult alreadySaved() {
            return new ConfirmationResult("already-saved", null, "这道菜已经保存过了，不会重复保存");
        }

        static ConfirmationResult processing() {
            return new ConfirmationResult("processing", null, "保存操作正在处理中，请稍后查看我的菜谱");
        }
    }
}
