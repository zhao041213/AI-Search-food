package com.example.food.pantry;

import com.example.food.common.ApiResponse;
import com.example.food.pantry.dto.CookingConsumptionRequest;
import com.example.food.pantry.dto.CookingPreviewResponse;
import com.example.food.pantry.dto.PantryOperationResponse;
import com.example.food.pantry.dto.StockInRequest;
import com.example.food.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/pantry")
public class PantryOperationController {

    private final PantryOperationService service;

    public PantryOperationController(PantryOperationService service) {
        this.service = service;
    }

    @GetMapping("/cooking-preview")
    public ApiResponse<CookingPreviewResponse> cookingPreview(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam Long recipeId,
            @RequestParam(required = false) Integer actualServings
    ) {
        return ApiResponse.ok(service.cookingPreview(principal.id(), recipeId, actualServings));
    }

    @PostMapping("/cooking-consumptions")
    public ApiResponse<PantryOperationResponse> consume(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CookingConsumptionRequest request
    ) {
        return ApiResponse.ok(service.consume(principal.id(), request));
    }

    @PostMapping("/stock-ins")
    public ApiResponse<PantryOperationResponse> stockIn(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody StockInRequest request
    ) {
        return ApiResponse.ok(service.stockIn(principal.id(), request));
    }

    @GetMapping("/operations")
    public ApiResponse<List<PantryOperationResponse>> recent(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(service.recent(principal.id(), limit));
    }

    @PostMapping("/operations/{id}/undo")
    public ApiResponse<PantryOperationResponse> undo(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) String idempotencyKey
    ) {
        return ApiResponse.ok(service.undo(principal.id(), id, idempotencyKey));
    }
}
