package com.example.food.auth;

import com.example.food.common.ApiResponse;
import com.example.food.security.AuthPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class MeController {

    @GetMapping("/me")
    public ApiResponse<AuthPrincipal> me(Authentication authentication) {
        return ApiResponse.ok((AuthPrincipal) authentication.getPrincipal());
    }
}
