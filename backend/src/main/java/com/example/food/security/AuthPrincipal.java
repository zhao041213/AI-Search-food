package com.example.food.security;

public record AuthPrincipal(
        Long id,
        String username,
        AppRole role,
        Long authVersion
) {
    public AuthPrincipal(Long id, String username, AppRole role) {
        this(id, username, role, null);
    }
}
