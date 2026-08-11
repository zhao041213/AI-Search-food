package com.example.food.stats;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IngredientNormalizerTest {

    private final IngredientNormalizer normalizer = new IngredientNormalizer();

    @Test
    void normalizesSeparatorsAliasesAndDuplicateCanonicalNames() {
        List<IngredientNormalizer.NormalizedIngredient> result = normalizer.normalizeDistinct(
                " 西红柿，鸡蛋\n番茄；马铃薯、土豆 "
        );

        assertThat(result)
                .extracting(
                        IngredientNormalizer.NormalizedIngredient::originalName,
                        IngredientNormalizer.NormalizedIngredient::canonicalName
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("西红柿", "番茄"),
                        org.assertj.core.groups.Tuple.tuple("鸡蛋", "鸡蛋"),
                        org.assertj.core.groups.Tuple.tuple("马铃薯", "土豆")
                );
    }

    @Test
    void ignoresBlankValuesAndSupportsCommonFoodAliases() {
        List<IngredientNormalizer.NormalizedIngredient> result = normalizer.normalizeDistinct(
                " ；花椰菜,西兰花,青椒,柿子椒, "
        );

        assertThat(result)
                .extracting(IngredientNormalizer.NormalizedIngredient::canonicalName)
                .containsExactly("西兰花", "青椒");
    }

    @Test
    void returnsEmptyListForBlankInput() {
        assertThat(normalizer.normalizeDistinct(" \n，； ")).isEmpty();
        assertThat(normalizer.normalizeDistinct(null)).isEmpty();
    }
}
