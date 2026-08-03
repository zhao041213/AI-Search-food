package com.example.food.auth.dto;

public record PhoneCodeResponse(
        String code,
        long retryAfterSeconds
) {
}
