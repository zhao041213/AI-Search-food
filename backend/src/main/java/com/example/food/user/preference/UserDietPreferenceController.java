package com.example.food.user.preference;

import com.example.food.common.ApiResponse;
import com.example.food.security.AuthPrincipal;
import com.example.food.user.preference.dto.DietPreferenceRequest;
import com.example.food.user.preference.dto.DietPreferenceResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/diet-preferences")
public class UserDietPreferenceController {

    private final UserDietPreferenceService service;

    public UserDietPreferenceController(UserDietPreferenceService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<DietPreferenceResponse> get(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ApiResponse.ok(service.get(principal.id()));
    }

    @PutMapping
    public ApiResponse<DietPreferenceResponse> save(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody DietPreferenceRequest request
    ) {
        return ApiResponse.ok(service.save(principal.id(), request));
    }
}
