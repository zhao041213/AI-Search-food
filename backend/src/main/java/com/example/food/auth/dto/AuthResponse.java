package com.example.food.auth.dto;

public record AuthResponse(
        String token,
        Long id,
        String displayName,
        String role
) {
}
