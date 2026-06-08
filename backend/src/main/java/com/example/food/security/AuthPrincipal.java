package com.example.food.security;

public record AuthPrincipal(
        Long id,
        String username,
        AppRole role
) {
}
