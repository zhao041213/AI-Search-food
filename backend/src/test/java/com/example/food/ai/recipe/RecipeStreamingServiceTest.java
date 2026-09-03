package com.example.food.ai.recipe;

import com.example.food.ai.qwen.QwenRecipeClient;
import com.example.food.ai.recipe.dto.RecipeGenerateRequest;
import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecipeStreamingServiceTest {

    private final RecipeRecommendationService recommendationService = mock(RecipeRecommendationService.class);
    private final QwenRecipeClient qwenClient = mock(QwenRecipeClient.class);
    private final RecipeStreamingService service = new RecipeStreamingService(recommendationService, qwenClient);

    @Test
    void persistsExactlyOnceAfterACompleteRecipe() {
        RecipeGenerateResponse response = recipe().withSearchLogId(19L);
        when(recommendationService.promptFor(any(), any())).thenReturn("prompt");
        when(recommendationService.persist(any(), any(), any(), anyString())).thenReturn(response);
        when(qwenClient.streamRecipe(anyString(), any(), any())).thenAnswer(invocation -> {
            Consumer<String> onDelta = invocation.getArgument(1);
            onDelta.accept("{\"title\":\"番茄炒蛋\",\"summary\":\"家常菜\",\"ingredients\":[{\"name\":\"番茄\"}],\"steps\":[{\"title\":\"炒制\",\"description\":\"炒熟\"}]}");
            return new QwenRecipeClient.RecipeStreamResult("", response, "qwen", "qwen-plus", false);
        });

        SseEmitter emitter = service.generate(
                new RecipeGenerateRequest("番茄、鸡蛋", "dinner", "light", "text"),
                null,
                "anon-001"
        );

        verify(recommendationService, timeout(2000).times(1)).persist(any(), any(), any(), anyString());
        verify(qwenClient, timeout(2000)).streamRecipe(anyString(), any(), any());
        emitter.complete();
    }

    @Test
    void doesNotPersistAnIncompleteRecipe() {
        RecipeGenerateResponse response = new RecipeGenerateResponse(
                "",
                "",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "qwen",
                "qwen-plus"
        );
        when(recommendationService.promptFor(any(), any())).thenReturn("prompt");
        when(qwenClient.streamRecipe(anyString(), any(), any())).thenReturn(
                new QwenRecipeClient.RecipeStreamResult("", response, "qwen", "qwen-plus", false)
        );

        service.generate(new RecipeGenerateRequest("番茄", "dinner", "light", "text"), null, "anon-001");

        verify(qwenClient, timeout(2000)).streamRecipe(anyString(), any(), any());
        verify(recommendationService, never()).persist(any(), any(), any(), anyString());
    }

    private RecipeGenerateResponse recipe() {
        return new RecipeGenerateResponse(
                "番茄炒蛋",
                "家常菜",
                List.of(),
                List.of(new RecipeGenerateResponse.Ingredient("番茄", "2个")),
                List.of(new RecipeGenerateResponse.Step(1, "炒制", "炒熟", 5)),
                List.of(),
                List.of(),
                "qwen",
                "qwen-plus"
        );
    }
}
