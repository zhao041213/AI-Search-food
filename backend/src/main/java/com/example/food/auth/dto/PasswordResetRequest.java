package com.example.food.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordResetRequest(
        @NotBlank
        @Pattern(regexp = "^1[3-9]\\d{9}$")
        String phone,
        @NotBlank
        @Pattern(regexp = "^\\d{6}$")
        String code,
        @NotBlank
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[\\s\\S]{8,64}$")
        String newPassword
) {
}
