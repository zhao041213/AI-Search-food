package com.example.food.user.character.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public record KitchenCharacterNamesResponse(
        Map<String, String> names,
        boolean hasCustomNames
) {

    public KitchenCharacterNamesResponse {
        names = Map.copyOf(new LinkedHashMap<>(names));
    }
}
