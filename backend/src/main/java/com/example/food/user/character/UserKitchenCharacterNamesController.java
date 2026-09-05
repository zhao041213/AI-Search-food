package com.example.food.user.character;

import com.example.food.common.ApiResponse;
import com.example.food.security.AuthPrincipal;
import com.example.food.user.character.dto.KitchenCharacterNamesRequest;
import com.example.food.user.character.dto.KitchenCharacterNamesResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/kitchen-character-names")
public class UserKitchenCharacterNamesController {

    private final UserKitchenCharacterNamesService service;

    public UserKitchenCharacterNamesController(UserKitchenCharacterNamesService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<KitchenCharacterNamesResponse> get(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ApiResponse.ok(service.get(principal.id()));
    }

    @PutMapping
    public ApiResponse<KitchenCharacterNamesResponse> save(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody KitchenCharacterNamesRequest request
    ) {
        return ApiResponse.ok(service.save(principal.id(), request));
    }

    @DeleteMapping
    public ApiResponse<KitchenCharacterNamesResponse> reset(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ApiResponse.ok(service.reset(principal.id()));
    }
}
