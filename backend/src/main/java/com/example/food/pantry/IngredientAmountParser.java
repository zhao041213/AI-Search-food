package com.example.food.pantry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses only conservative ingredient amounts; unknown text is never treated as zero. */
public final class IngredientAmountParser {

    private static final Pattern SIMPLE_AMOUNT = Pattern.compile(
            "^\\s*(\\d+(?:\\.\\d+)?|\\d+\\s*/\\s*\\d+)\\s*([\\p{L}]+|个|只|枚|颗|片|根|瓣|包|袋|盒|杯|汤匙|茶匙|勺|块|瓶|罐)\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Map<String, String> UNIT_ALIASES = Map.ofEntries(
            Map.entry("克", "g"), Map.entry("g", "g"), Map.entry("gram", "g"), Map.entry("grams", "g"),
            Map.entry("千克", "kg"), Map.entry("公斤", "kg"), Map.entry("kg", "kg"),
            Map.entry("毫升", "ml"), Map.entry("ml", "ml"), Map.entry("milliliter", "ml"), Map.entry("毫升数", "ml"),
            Map.entry("升", "l"), Map.entry("l", "l"), Map.entry("liter", "l"),
            Map.entry("个", "个"), Map.entry("只", "只"), Map.entry("枚", "枚"), Map.entry("颗", "颗"),
            Map.entry("片", "片"), Map.entry("根", "根"), Map.entry("瓣", "瓣"), Map.entry("包", "包"),
            Map.entry("袋", "袋"), Map.entry("盒", "盒"), Map.entry("杯", "杯"), Map.entry("把", "把"), Map.entry("条", "条"), Map.entry("碗", "碗"), Map.entry("汤匙", "汤匙"),
            Map.entry("tbsp", "汤匙"), Map.entry("茶匙", "茶匙"), Map.entry("tsp", "茶匙"), Map.entry("勺", "勺"),
            Map.entry("块", "块"), Map.entry("瓶", "瓶"), Map.entry("罐", "罐")
    );

    public ParsedAmount parse(String rawAmount) {
        String raw = rawAmount == null ? "" : rawAmount.trim();
        Matcher matcher = SIMPLE_AMOUNT.matcher(raw);
        if (!matcher.matches()) {
            return containsUnquantifiedMarker(raw)
                    ? new ParsedAmount(Status.UNQUANTIFIED, null, null, raw)
                    : new ParsedAmount(Status.INVALID, null, null, raw);
        }
        BigDecimal quantity = parseNumber(matcher.group(1));
        String unit = canonicalUnit(matcher.group(2));
        if (quantity == null || quantity.signum() <= 0 || unit == null) {
            return new ParsedAmount(Status.INVALID, null, unit, raw);
        }
        return new ParsedAmount(Status.PARSED, quantity, unit, raw);
    }

    public ParsedAmount scale(ParsedAmount amount, int actualServings, int defaultServings) {
        if (amount == null || !amount.isParsed() || actualServings < 1 || defaultServings < 1) {
            return new ParsedAmount(Status.UNQUANTIFIED, null, null, amount == null ? "" : amount.rawAmount());
        }
        BigDecimal scaled = amount.quantity()
                .multiply(BigDecimal.valueOf(actualServings))
                .divide(BigDecimal.valueOf(defaultServings), 8, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return new ParsedAmount(Status.PARSED, scaled, amount.unit(), amount.rawAmount());
    }

    public boolean compatible(String leftUnit, String rightUnit) {
        String left = canonicalUnit(leftUnit);
        String right = canonicalUnit(rightUnit);
        if (left == null || right == null) {
            return false;
        }
        return dimension(left).equals(dimension(right));
    }

    public BigDecimal convert(BigDecimal quantity, String fromUnit, String toUnit) {
        if (quantity == null || !compatible(fromUnit, toUnit)) {
            return null;
        }
        String from = canonicalUnit(fromUnit);
        String to = canonicalUnit(toUnit);
        if (from.equals(to)) {
            return quantity;
        }
        if (from.equals("kg") && to.equals("g")) return quantity.multiply(BigDecimal.valueOf(1000));
        if (from.equals("g") && to.equals("kg")) return quantity.divide(BigDecimal.valueOf(1000), 8, RoundingMode.HALF_UP);
        if (from.equals("l") && to.equals("ml")) return quantity.multiply(BigDecimal.valueOf(1000));
        if (from.equals("ml") && to.equals("l")) return quantity.divide(BigDecimal.valueOf(1000), 8, RoundingMode.HALF_UP);
        return null;
    }

    public String canonicalUnit(String unit) {
        if (unit == null || unit.isBlank()) return null;
        return UNIT_ALIASES.get(unit.trim().toLowerCase(Locale.ROOT));
    }

    private String dimension(String unit) {
        return switch (unit) {
            case "g", "kg" -> "mass";
            case "ml", "l" -> "volume";
            default -> "count:" + unit;
        };
    }

    private boolean containsUnquantifiedMarker(String raw) {
        return raw.contains("少许") || raw.contains("适量") || raw.contains("若干") || raw.contains("少量")
                || raw.contains("适度") || raw.contains("少量");
    }

    private BigDecimal parseNumber(String value) {
        try {
            if (!value.contains("/")) return new BigDecimal(value);
            String[] parts = value.split("/");
            BigDecimal denominator = new BigDecimal(parts[1].trim());
            if (denominator.signum() == 0) return null;
            return new BigDecimal(parts[0].trim()).divide(denominator, 8, RoundingMode.HALF_UP);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public enum Status { PARSED, UNQUANTIFIED, INVALID }

    public record ParsedAmount(Status status, BigDecimal quantity, String unit, String rawAmount) {
        public boolean isParsed() {
            return status == Status.PARSED && quantity != null && quantity.signum() > 0 && unit != null;
        }
    }
}
