package com.example.food.ai.recipe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecipeGenerateResponse(
        String title,
        String summary,
        List<String> effects,
        List<Ingredient> ingredients,
        List<MissingIngredient> missingIngredients,
        List<Step> steps,
        List<String> tips,
        List<String> videoKeywords,
        Explanation explanation,
        NutritionEstimate nutritionEstimate,
        String provider,
        String model,
        Long searchLogId
) {
    public RecipeGenerateResponse(
            String title,
            String summary,
            List<String> effects,
            List<Ingredient> ingredients,
            List<MissingIngredient> missingIngredients,
            List<Step> steps,
            List<String> tips,
            List<String> videoKeywords,
            Explanation explanation,
            String provider,
            String model
    ) {
        this(title, summary, effects, ingredients, missingIngredients, steps, tips, videoKeywords,
                explanation, null, provider, model, null);
    }

    public RecipeGenerateResponse(
            String title,
            String summary,
            List<String> effects,
            List<Ingredient> ingredients,
            List<MissingIngredient> missingIngredients,
            List<Step> steps,
            List<String> tips,
            List<String> videoKeywords,
            Explanation explanation,
            String provider,
            String model,
            Long searchLogId
    ) {
        this(title, summary, effects, ingredients, missingIngredients, steps, tips, videoKeywords,
                explanation, null, provider, model, searchLogId);
    }

    public RecipeGenerateResponse(
            String title,
            String summary,
            List<String> effects,
            List<Ingredient> ingredients,
            List<Step> steps,
            List<String> tips,
            List<String> videoKeywords,
            String provider,
            String model
    ) {
        this(title, summary, effects, ingredients, List.of(), steps, tips, videoKeywords,
                Explanation.empty(), null, provider, model, null);
    }

    public RecipeGenerateResponse(
            String title,
            String summary,
            List<String> effects,
            List<Ingredient> ingredients,
            List<Step> steps,
            List<String> tips,
            List<String> videoKeywords,
            String provider,
            String model,
            Long searchLogId
    ) {
        this(title, summary, effects, ingredients, List.of(), steps, tips, videoKeywords,
                Explanation.empty(), null, provider, model, searchLogId);
    }

    public RecipeGenerateResponse(
            String title,
            String summary,
            List<String> effects,
            List<Ingredient> ingredients,
            List<MissingIngredient> missingIngredients,
            List<Step> steps,
            List<String> tips,
            List<String> videoKeywords,
            Explanation explanation,
            NutritionEstimate nutritionEstimate,
            String provider,
            String model
    ) {
        this(title, summary, effects, ingredients, missingIngredients, steps, tips, videoKeywords,
                explanation, nutritionEstimate, provider, model, null);
    }

    public RecipeGenerateResponse {
        effects = effects == null ? List.of() : List.copyOf(effects);
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        missingIngredients = missingIngredients == null ? List.of() : List.copyOf(missingIngredients);
        steps = steps == null ? List.of() : List.copyOf(steps);
        tips = tips == null ? List.of() : List.copyOf(tips);
        videoKeywords = videoKeywords == null ? List.of() : List.copyOf(videoKeywords);
        explanation = explanation == null ? Explanation.empty() : explanation;
    }

    public RecipeGenerateResponse withSearchLogId(Long id) {
        return new RecipeGenerateResponse(
                title,
                summary,
                effects,
                ingredients,
                missingIngredients,
                steps,
                tips,
                videoKeywords,
                explanation,
                nutritionEstimate,
                provider,
                model,
                id
        );
    }

    public RecipeGenerateResponse withNutritionEstimate(NutritionEstimate estimate) {
        return new RecipeGenerateResponse(
                title,
                summary,
                effects,
                ingredients,
                missingIngredients,
                steps,
                tips,
                videoKeywords,
                explanation,
                estimate,
                provider,
                model,
                searchLogId
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ingredient(
            String name,
            String amount
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MissingIngredient(
            String name,
            String amount,
            List<String> substitutes,
            String reason
    ) {
        public MissingIngredient {
            name = safeText(name);
            amount = safeText(amount);
            substitutes = substitutes == null ? List.of() : List.copyOf(substitutes);
            reason = safeText(reason);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Explanation(
            String pairingLogic,
            String nutrition,
            String cookingPrinciple
    ) {
        public Explanation {
            pairingLogic = safeText(pairingLogic);
            nutrition = safeText(nutrition);
            cookingPrinciple = safeText(cookingPrinciple);
        }

        public static Explanation empty() {
            return new Explanation("", "", "");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NutritionEstimate(
            Integer servings,
            BigDecimal caloriesKcal,
            BigDecimal proteinG,
            BigDecimal fatG,
            BigDecimal carbohydrateG,
            String source
    ) {
        public static final String AI_ESTIMATE = "AI_ESTIMATE";

        public boolean isValid() {
            return servings != null && servings >= 1 && servings <= 20
                    && inRange(caloriesKcal, BigDecimal.ONE, new BigDecimal("3000"))
                    && inRange(proteinG, BigDecimal.ZERO, new BigDecimal("300"))
                    && inRange(fatG, BigDecimal.ZERO, new BigDecimal("300"))
                    && inRange(carbohydrateG, BigDecimal.ZERO, new BigDecimal("500"))
                    && AI_ESTIMATE.equals(source);
        }

        public static NutritionEstimate from(JsonNode node) {
            if (node == null || !node.isObject()) {
                return null;
            }
            Integer servings = integer(node, "servings");
            BigDecimal calories = decimal(node, "caloriesKcal");
            BigDecimal protein = decimal(node, "proteinG");
            BigDecimal fat = decimal(node, "fatG");
            BigDecimal carbohydrate = decimal(node, "carbohydrateG");
            NutritionEstimate estimate = new NutritionEstimate(
                    servings, calories, protein, fat, carbohydrate, AI_ESTIMATE
            );
            return estimate.isValid() ? estimate : null;
        }

        private static boolean inRange(BigDecimal value, BigDecimal min, BigDecimal max) {
            return value != null && value.scale() <= 6
                    && value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
        }

        private static Integer integer(JsonNode node, String field) {
            JsonNode value = node.get(field);
            if (value == null || !value.isIntegralNumber()) {
                return null;
            }
            return value.canConvertToInt() ? value.intValue() : null;
        }

        private static BigDecimal decimal(JsonNode node, String field) {
            JsonNode value = node.get(field);
            if (value == null || !value.isNumber()) {
                return null;
            }
            try {
                return value.decimalValue().setScale(Math.min(value.decimalValue().scale(), 6), RoundingMode.UNNECESSARY);
            } catch (ArithmeticException exception) {
                return null;
            }
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Step(
            int order,
            String title,
            String description,
            Integer durationMinutes
    ) {
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }
}
