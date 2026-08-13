package com.example.food.ai.recipe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
                explanation, provider, model, null);
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
                Explanation.empty(), provider, model, null);
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
                Explanation.empty(), provider, model, searchLogId);
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
                provider,
                model,
                id
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
