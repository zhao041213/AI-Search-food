package com.example.food.user.nutrition;

import com.example.food.common.ApiResponse;
import com.example.food.security.AuthPrincipal;
import com.example.food.user.nutrition.dto.NutritionTargetRequest;
import com.example.food.user.nutrition.dto.NutritionTargetResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/nutrition-target")
public class UserNutritionTargetController {

    private final UserNutritionTargetService service;

    public UserNutritionTargetController(UserNutritionTargetService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<NutritionTargetResponse> get(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ApiResponse.ok(service.get(principal.id()));
    }

    @PutMapping
    public ApiResponse<NutritionTargetResponse> save(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody NutritionTargetRequest request
    ) {
        return ApiResponse.ok(service.save(principal.id(), request));
    }

    @DeleteMapping
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        service.delete(principal.id());
        return ApiResponse.ok(null);
    }
}
