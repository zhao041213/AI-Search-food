package com.example.food.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneCodeRequest(
        @NotBlank
        @Pattern(regexp = "^1[3-9]\\d{9}$")
        String phone
) {
}
