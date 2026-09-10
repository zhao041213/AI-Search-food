package com.example.food.user.healthnutrition;

import com.example.food.common.ApiResponse;
import com.example.food.security.AuthPrincipal;
import com.example.food.user.healthnutrition.dto.HealthNutritionProfileRequest;
import com.example.food.user.healthnutrition.dto.HealthNutritionProfileResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/health-nutrition")
public class HealthNutritionController {

    private final HealthNutritionService service;

    public HealthNutritionController(HealthNutritionService service) { this.service = service; }

    @GetMapping
    public ApiResponse<HealthNutritionProfileResponse> get(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(service.get(principal.id()));
    }

    @PutMapping
    public ApiResponse<HealthNutritionProfileResponse> save(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody HealthNutritionProfileRequest request
    ) {
        return ApiResponse.ok(service.save(principal.id(), request));
    }

    @PostMapping("/ai-reference")
    public ApiResponse<HealthNutritionProfileResponse> generateAiReference(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(service.generateAiReference(principal.id()));
    }
}
