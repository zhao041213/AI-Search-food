package com.example.food.user.health;

import com.example.food.common.ApiResponse;
import com.example.food.security.AuthPrincipal;
import com.example.food.user.health.dto.HealthProfileRequest;
import com.example.food.user.health.dto.HealthProfileResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/health-profile")
public class UserHealthProfileController {

    private final UserHealthProfileService service;

    public UserHealthProfileController(UserHealthProfileService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<HealthProfileResponse> get(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ApiResponse.ok(service.get(principal.id()));
    }

    @PutMapping
    public ApiResponse<HealthProfileResponse> save(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody HealthProfileRequest request
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
