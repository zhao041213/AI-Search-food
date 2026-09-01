package com.example.food.ai.recipe;

import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NutritionEstimateTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsCompleteEstimateWithinDocumentedRanges() throws Exception {
        RecipeGenerateResponse.NutritionEstimate estimate = RecipeGenerateResponse.NutritionEstimate.from(
                objectMapper.readTree("""
                        {"servings": 2, "caloriesKcal": 420.5, "proteinG": 24,
                         "fatG": 16, "carbohydrateG": 42, "source": "AI_ESTIMATE"}
                        """)
        );

        assertThat(estimate).isNotNull();
        assertThat(estimate.caloriesKcal()).isEqualByComparingTo("420.5");
    }

    @Test
    void rejectsMissingNegativeExtremeAndNonNumericValuesAsOneInvalidEstimate() throws Exception {
        assertThat(RecipeGenerateResponse.NutritionEstimate.from(objectMapper.readTree(
                "{\"servings\":2,\"caloriesKcal\":0,\"proteinG\":24,\"fatG\":16,\"carbohydrateG\":42,\"source\":\"AI_ESTIMATE\"}"
        ))).isNull();
        assertThat(RecipeGenerateResponse.NutritionEstimate.from(objectMapper.readTree(
                "{\"servings\":2,\"caloriesKcal\":420,\"proteinG\":-1,\"fatG\":16,\"carbohydrateG\":42,\"source\":\"AI_ESTIMATE\"}"
        ))).isNull();
        assertThat(RecipeGenerateResponse.NutritionEstimate.from(objectMapper.readTree(
                "{\"servings\":2,\"caloriesKcal\":420,\"proteinG\":24,\"fatG\":16,\"carbohydrateG\":\"unknown\",\"source\":\"AI_ESTIMATE\"}"
        ))).isNull();
    }
}
