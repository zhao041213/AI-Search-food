package com.example.food.recipe.collection;

import com.example.food.common.ApiResponse;
import com.example.food.recipe.collection.dto.BatchOperationResponse;
import com.example.food.recipe.collection.dto.RecipeCollectionRequest;
import com.example.food.recipe.collection.dto.RecipeCollectionResponse;
import com.example.food.recipe.collection.dto.RecipeTagResponse;
import com.example.food.recipe.collection.dto.SavedRecipeBatchDeleteRequest;
import com.example.food.recipe.collection.dto.SavedRecipeBatchMoveRequest;
import com.example.food.recipe.collection.dto.SavedRecipeBatchTagsRequest;
import com.example.food.recipe.collection.dto.SavedRecipeCollectionRequest;
import com.example.food.recipe.collection.dto.SavedRecipePageResponse;
import com.example.food.recipe.collection.dto.SavedRecipeTagsRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me")
public class SavedRecipeCollectionController {

    private final SavedRecipeCollectionService service;

    public SavedRecipeCollectionController(SavedRecipeCollectionService service) {
        this.service = service;
    }

    @GetMapping("/recipe-collections")
    public ApiResponse<List<RecipeCollectionResponse>> collections(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ApiResponse.ok(service.listCollections(principal.id()));
    }

    @PostMapping("/recipe-collections")
    public ApiResponse<RecipeCollectionResponse> createCollection(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody RecipeCollectionRequest request
    ) {
        return ApiResponse.ok(service.createCollection(principal.id(), request));
    }

    @PutMapping("/recipe-collections/{id}")
    public ApiResponse<RecipeCollectionResponse> renameCollection(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody RecipeCollectionRequest request
    ) {
        return ApiResponse.ok(service.renameCollection(principal.id(), id, request));
    }

    @DeleteMapping("/recipe-collections/{id}")
    public ApiResponse<Void> deleteCollection(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean confirm
    ) {
        service.deleteCollection(principal.id(), id, confirm);
        return ApiResponse.ok(null);
    }

    @GetMapping("/saved-recipes")
    public ApiResponse<SavedRecipePageResponse> savedRecipes(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false) Long collectionId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String mealType,
            @RequestParam(required = false) String goal,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "savedAtDesc") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.listSavedRecipes(
                principal.id(), collectionId, keyword, mealType, goal, tag, sort, page, size));
    }

    @GetMapping("/recipe-tags")
    public ApiResponse<List<RecipeTagResponse>> tags(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ApiResponse.ok(service.listTags(principal.id()));
    }

    @PutMapping("/saved-recipes/{id}/collection")
    public ApiResponse<Void> moveRecipe(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody SavedRecipeCollectionRequest request
    ) {
        service.moveRecipe(principal.id(), id, request);
        return ApiResponse.ok(null);
    }

    @PutMapping("/saved-recipes/{id}/tags")
    public ApiResponse<Void> replaceTags(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody SavedRecipeTagsRequest request
    ) {
        service.replaceTags(principal.id(), id, request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/saved-recipes/batch-move")
    public ApiResponse<BatchOperationResponse> batchMove(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody SavedRecipeBatchMoveRequest request
    ) {
        return ApiResponse.ok(service.batchMove(principal.id(), request));
    }

    @PostMapping("/saved-recipes/batch-tags")
    public ApiResponse<BatchOperationResponse> batchTags(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody SavedRecipeBatchTagsRequest request
    ) {
        return ApiResponse.ok(service.batchTags(principal.id(), request));
    }

    @DeleteMapping("/saved-recipes/batch")
    public ApiResponse<BatchOperationResponse> batchDelete(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody SavedRecipeBatchDeleteRequest request
    ) {
        return ApiResponse.ok(service.batchDelete(principal.id(), request));
    }
}
