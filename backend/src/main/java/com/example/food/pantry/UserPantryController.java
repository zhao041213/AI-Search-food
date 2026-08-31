package com.example.food.pantry;

import com.example.food.common.ApiResponse;
import com.example.food.pantry.dto.ConsumePantryItemRequest;
import com.example.food.pantry.dto.PantryItemRequest;
import com.example.food.pantry.dto.PantryItemResponse;
import com.example.food.pantry.dto.PantryExpirySummaryResponse;
import com.example.food.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/pantry")
public class UserPantryController {

    private final UserPantryService service;

    public UserPantryController(UserPantryService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<PantryItemResponse>> list(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ApiResponse.ok(service.list(principal.id()));
    }

    @GetMapping("/expiry-alerts")
    public ApiResponse<PantryExpirySummaryResponse> expiryAlerts(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ApiResponse.ok(service.expirySummary(principal.id()));
    }

    @PostMapping
    public ApiResponse<PantryItemResponse> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody PantryItemRequest request
    ) {
        return ApiResponse.ok(service.create(principal.id(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PantryItemResponse> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody PantryItemRequest request
    ) {
        return ApiResponse.ok(service.update(principal.id(), id, request));
    }

    @PostMapping("/{id}/consume")
    public ApiResponse<PantryItemResponse> consume(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ConsumePantryItemRequest request
    ) {
        return ApiResponse.ok(service.consume(principal.id(), id, request.quantity()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id
    ) {
        service.delete(principal.id(), id);
        return ApiResponse.ok(null);
    }
}
