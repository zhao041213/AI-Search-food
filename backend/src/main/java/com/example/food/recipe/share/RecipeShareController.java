package com.example.food.recipe.share;

import com.example.food.common.ApiResponse;
import com.example.food.recipe.share.dto.PublicRecipeResponse;
import com.example.food.recipe.share.dto.RecipeShareCreateRequest;
import com.example.food.recipe.share.dto.RecipeShareResponse;
import com.example.food.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RecipeShareController {

    private final RecipeShareService service;

    public RecipeShareController(RecipeShareService service) {
        this.service = service;
    }

    @PostMapping("/api/users/me/saved-recipes/{id}/shares")
    public ApiResponse<RecipeShareResponse> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody RecipeShareCreateRequest request
    ) {
        return ApiResponse.ok(service.createShare(principal.id(), id, request));
    }

    @GetMapping("/api/users/me/recipe-shares")
    public ApiResponse<List<RecipeShareResponse>> list(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ApiResponse.ok(service.listShares(principal.id()));
    }

    @PutMapping("/api/users/me/recipe-shares/{id}/disable")
    public ApiResponse<Void> disable(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id
    ) {
        service.disableShare(principal.id(), id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/shared/recipes/{token}")
    public ApiResponse<PublicRecipeResponse> publicRecipe(@PathVariable String token) {
        return ApiResponse.ok(service.publicRecipe(token));
    }
}
