package com.example.food.stats;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class IngredientNormalizer {

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("西红柿", "番茄"),
            Map.entry("马铃薯", "土豆"),
            Map.entry("花椰菜", "西兰花"),
            Map.entry("菜花", "西兰花"),
            Map.entry("柿子椒", "青椒")
    );

    public List<NormalizedIngredient> normalizeDistinct(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }

        Map<String, NormalizedIngredient> ingredients = new LinkedHashMap<>();
        for (String part : value.split("[,，、;；\\r\\n]+")) {
            String originalName = part.trim();
            if (!StringUtils.hasText(originalName)) {
                continue;
            }
            String canonicalName = ALIASES.getOrDefault(originalName, originalName);
            ingredients.putIfAbsent(
                    canonicalName,
                    new NormalizedIngredient(originalName, canonicalName)
            );
        }
        return new ArrayList<>(ingredients.values());
    }

    public record NormalizedIngredient(String originalName, String canonicalName) {
    }
}
