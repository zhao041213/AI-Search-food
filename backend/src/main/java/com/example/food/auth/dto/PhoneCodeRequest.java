package com.example.food.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PhoneCodeRequest(
        @NotBlank String phone
) {
}
