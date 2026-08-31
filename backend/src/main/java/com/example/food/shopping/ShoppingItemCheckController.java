package com.example.food.shopping;

import com.example.food.common.ApiResponse;
import com.example.food.security.AuthPrincipal;
import com.example.food.shopping.dto.ShoppingItemCheckRequest;
import com.example.food.shopping.dto.ShoppingItemCheckResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/shopping-checks")
public class ShoppingItemCheckController {

    private final ShoppingItemCheckService service;

    public ShoppingItemCheckController(ShoppingItemCheckService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ShoppingItemCheckResponse>> list(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam Long searchLogId
    ) {
        return ApiResponse.ok(service.list(principal.id(), searchLogId));
    }

    @PutMapping
    public ApiResponse<ShoppingItemCheckResponse> save(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody ShoppingItemCheckRequest request
    ) {
        return ApiResponse.ok(service.save(principal.id(), request));
    }
}
