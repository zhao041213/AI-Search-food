package com.example.food.review;

import com.example.food.common.ApiResponse;
import com.example.food.review.dto.FinishedDishReviewRequest;
import com.example.food.review.dto.FinishedDishReviewResponse;
import com.example.food.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping
public class FinishedDishReviewController {

    private final FinishedDishReviewService reviewService;

    public FinishedDishReviewController(FinishedDishReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping(value = "/api/ai/finished-dish-reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FinishedDishReviewResponse> create(
            @Valid @RequestPart("request") FinishedDishReviewRequest request,
            @RequestPart("image") MultipartFile image,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ApiResponse.ok(reviewService.create(request, image, principal));
    }

    @GetMapping("/api/finished-dish-reviews")
    public ApiResponse<List<FinishedDishReviewResponse>> list(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false) Long recipeId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.ok(reviewService.list(principal.id(), recipeId, limit));
    }

    @DeleteMapping("/api/finished-dish-reviews/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id
    ) {
        reviewService.delete(principal.id(), id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/finished-dish-reviews/{id}/image")
    public ResponseEntity<byte[]> image(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id
    ) {
        FinishedDishReviewService.ReviewImage image = reviewService.loadOwnedImage(principal.id(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(image.originalName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(image.bytes());
    }
}
