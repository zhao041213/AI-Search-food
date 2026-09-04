package com.example.food.user.account.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AccountCancellationRequest(
        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码格式不正确")
        String code,
        @AssertTrue(message = "请确认注销后无法恢复当前账号数据")
        boolean confirmed
) {
}
