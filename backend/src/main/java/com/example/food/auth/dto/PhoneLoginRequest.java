package com.example.food.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PhoneLoginRequest(
        @NotBlank
        @Size(max = 32)
        @Pattern(regexp = "^\\+?\\d{6,32}$")
        String phone,
        @NotBlank
        @Size(max = 16)
        @Pattern(regexp = "^\\d{4,16}$")
        String code
) {
}
