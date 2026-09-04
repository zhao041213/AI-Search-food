package com.example.food.user.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @NotBlank(message = "昵称不能为空")
        @Size(min = 2, max = 64, message = "昵称长度需为 2-64 个字符")
        String nickname
) {
}
