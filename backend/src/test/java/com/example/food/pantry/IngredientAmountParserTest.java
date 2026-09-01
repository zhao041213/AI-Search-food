package com.example.food.pantry;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class IngredientAmountParserTest {

    private final IngredientAmountParser parser = new IngredientAmountParser();

    @Test
    void parsesSimpleDecimalAndFractionWithWhitelistedUnits() {
        var decimal = parser.parse(" 1.5 千克 ");
        var fraction = parser.parse("1/2杯");

        assertThat(decimal.isParsed()).isTrue();
        assertThat(decimal.quantity()).isEqualByComparingTo("1.5");
        assertThat(decimal.unit()).isEqualTo("kg");
        assertThat(fraction.isParsed()).isTrue();
        assertThat(fraction.quantity()).isEqualByComparingTo("0.5");
        assertThat(fraction.unit()).isEqualTo("杯");
    }

    @Test
    void scalesServingsAndConvertsOnlyMassAndVolume() {
        var amount = parser.parse("100克");

        var scaled = parser.scale(amount, 3, 2);

        assertThat(scaled.quantity()).isEqualByComparingTo("150");
        assertThat(parser.convert(new BigDecimal("1.25"), "kg", "g")).isEqualByComparingTo("1250");
        assertThat(parser.convert(new BigDecimal("1"), "个", "克")).isNull();
    }

    @Test
    void treatsUnquantifiedAndMalformedAmountsAsInvalidWithoutZero() {
        var unquantified = parser.parse("适量");
        var malformed = parser.parse("0克");

        assertThat(unquantified.status()).isEqualTo(IngredientAmountParser.Status.UNQUANTIFIED);
        assertThat(unquantified.quantity()).isNull();
        assertThat(malformed.status()).isEqualTo(IngredientAmountParser.Status.INVALID);
        assertThat(malformed.quantity()).isNull();
    }
}
