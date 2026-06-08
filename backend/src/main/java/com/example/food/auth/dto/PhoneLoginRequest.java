package com.example.food.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PhoneLoginRequest(
        @NotBlank String phone,
        @NotBlank String code
) {
}
