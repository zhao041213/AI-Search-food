package com.example.food.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PhoneCodeRequest(
        @NotBlank
        @Size(max = 32)
        @Pattern(regexp = "^\\+?\\d+$")
        String phone
) {
}
