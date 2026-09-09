package com.example.food.agent;

import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.example.food.recipe.SavedRecipeService;
import com.example.food.recipe.dto.RecipeHistoryDetailResponse;
import com.example.food.recipe.dto.SaveRecipeRequest;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.UserSecurityLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentWriteServiceTest {

    @Mock
    private AgentConfirmationMapper confirmationMapper;

    @Mock
    private SavedRecipeService savedRecipeService;

    @Mock
    private UserSecurityLogService securityLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AgentWriteService service;
    private final AuthPrincipal principal = new AuthPrincipal(7L, "13800138000", AppRole.USER);

    @BeforeEach
    void setUp() {
        service = new AgentWriteService(confirmationMapper, savedRecipeService, securityLogService, objectMapper);
    }

    @Test
    void savesRecipeOnceAfterAtomicClaimAndRecordsSecurityEvent() throws Exception {
        RecipeGenerateResponse recipe = recipe();
        AgentConfirmation confirmation = confirmation("PENDING", objectMapper.writeValueAsString(recipe));
        RecipeHistoryDetailResponse detail = new RecipeHistoryDetailResponse(
                88L, 42L, LocalDateTime.now(), "鸡蛋", "dinner", "balanced", recipe
        );
        when(confirmationMapper.findOwned(7L, 9L)).thenReturn(confirmation);
        when(confirmationMapper.claim(7L, 9L)).thenReturn(1);
        when(savedRecipeService.save(any(SaveRecipeRequest.class), eq(principal), isNull())).thenReturn(detail);

        AgentWriteService.ConfirmationResult result = service.saveRecipe(principal, 9L, "key-9");

        assertThat(result.status()).isEqualTo("saved");
        assertThat(result.detail()).isEqualTo(detail);
        ArgumentCaptor<SaveRecipeRequest> request = ArgumentCaptor.forClass(SaveRecipeRequest.class);
        verify(savedRecipeService).save(request.capture(), eq(principal), isNull());
        assertThat(request.getValue().searchLogId()).isEqualTo(42L);
        assertThat(request.getValue().title()).isEqualTo("番茄炒蛋");
        verify(confirmationMapper).markConfirmed(7L, 9L);
        verify(securityLogService).record(7L, "AGENT_SAVE_RECIPE", "/api/agent/chat/stream", "confirmationId=9, recipeId=88");
    }

    @Test
    void returnsAlreadySavedWithoutWritingAgain() throws Exception {
        AgentConfirmation confirmation = confirmation("CONFIRMED", objectMapper.writeValueAsString(recipe()));
        when(confirmationMapper.findOwned(7L, 9L)).thenReturn(confirmation);

        AgentWriteService.ConfirmationResult result = service.saveRecipe(principal, 9L, "key-9");

        assertThat(result.status()).isEqualTo("already-saved");
        verify(confirmationMapper, never()).claim(7L, 9L);
        verify(savedRecipeService, never()).save(any(), any(), any());
    }

    private AgentConfirmation confirmation(String status, String payload) {
        AgentConfirmation confirmation = new AgentConfirmation();
        confirmation.setId(9L);
        confirmation.setUserId(7L);
        confirmation.setActionType("SAVE_RECIPE");
        confirmation.setIdempotencyKey("key-9");
        confirmation.setPayloadJson(payload);
        confirmation.setStatus(status);
        confirmation.setCreatedAt(LocalDateTime.now());
        return confirmation;
    }

    private RecipeGenerateResponse recipe() {
        return new RecipeGenerateResponse(
                "番茄炒蛋",
                "家常快手菜",
                List.of("补充蛋白质"),
                List.of(new RecipeGenerateResponse.Ingredient("鸡蛋", "2个")),
                List.of(new RecipeGenerateResponse.Step(1, "炒制", "鸡蛋下锅炒熟。", 5)),
                List.of("少油烹饪"),
                List.of("番茄炒蛋"),
                "qwen",
                "qwen-plus",
                42L
        );
    }
}
