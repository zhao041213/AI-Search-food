package com.example.food.ai.recipe;

import com.example.food.ai.qwen.QwenRecipeClient;
import com.example.food.ai.recipe.dto.RecipeGenerateRequest;
import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.example.food.recipe.SearchLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeRecommendationServiceTest {

    @Mock
    private QwenRecipeClient qwenRecipeClient;

    @Mock
    private SearchLogService searchLogService;

    @InjectMocks
    private RecipeRecommendationService recipeRecommendationService;

    @Test
    void returnsSearchLogIdAfterSuccessfulGeneration() {
        RecipeGenerateRequest request = new RecipeGenerateRequest("番茄", "dinner", "balanced", "text");
        RecipeGenerateResponse generated = recipeResponse();
        when(qwenRecipeClient.generateRecipe(anyString())).thenReturn(generated);
        when(searchLogService.record(request, generated, null, "anonymous-12345678")).thenReturn(88L);

        RecipeGenerateResponse result = recipeRecommendationService.generate(
                request,
                null,
                "anonymous-12345678"
        );

        assertThat(result.searchLogId()).isEqualTo(88L);
        verify(searchLogService).record(request, generated, null, "anonymous-12345678");
    }

    @Test
    void doesNotRecordWhenAiGenerationThrows() {
        RecipeGenerateRequest request = new RecipeGenerateRequest("番茄", "dinner", "balanced", "text");
        when(qwenRecipeClient.generateRecipe(anyString())).thenThrow(new IllegalStateException("AI unavailable"));

        assertThatThrownBy(() -> recipeRecommendationService.generate(request, null, "anonymous-12345678"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AI unavailable");
        verifyNoInteractions(searchLogService);
    }

    private RecipeGenerateResponse recipeResponse() {
        return new RecipeGenerateResponse(
                "番茄炒蛋",
                "家常快手菜",
                List.of("补充蛋白质"),
                List.of(new RecipeGenerateResponse.Ingredient("番茄", "2个")),
                List.of(new RecipeGenerateResponse.Step(1, "备菜", "番茄切块", 5)),
                List.of("先炒鸡蛋"),
                List.of("番茄炒蛋教程"),
                "qwen",
                "qwen-plus"
        );
    }
}
