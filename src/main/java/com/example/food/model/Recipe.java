package com.example.food.model;

import java.util.List;

public record Recipe(
        String name,
        List<String> ingredients,
        List<String> steps
) {
}
