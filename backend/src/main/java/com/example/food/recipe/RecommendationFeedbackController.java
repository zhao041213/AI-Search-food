package com.example.food.recipe;

import com.example.food.common.ApiResponse;
import com.example.food.recipe.dto.RecommendationFeedbackResponse;
import com.example.food.recipe.dto.RecommendationReactionRequest;
import com.example.food.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendation-feedbacks")
public class RecommendationFeedbackController {

    private final RecommendationFeedbackService service;

    public RecommendationFeedbackController(RecommendationFeedbackService service) {
        this.service = service;
    }

    @GetMapping("/{searchLogId}")
    public ApiResponse<RecommendationFeedbackResponse> get(
            @PathVariable Long searchLogId,
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestHeader(value = "X-Anonymous-Id", required = false) String anonymousId
    ) {
        return ApiResponse.ok(service.get(searchLogId, principal, anonymousId));
    }

    @PutMapping("/{searchLogId}/reaction")
    public ApiResponse<RecommendationFeedbackResponse> setReaction(
            @PathVariable Long searchLogId,
            @Valid @RequestBody RecommendationReactionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestHeader(value = "X-Anonymous-Id", required = false) String anonymousId
    ) {
        return ApiResponse.ok(service.setReaction(searchLogId, request, principal, anonymousId));
    }

    @DeleteMapping("/{searchLogId}/reaction")
    public ApiResponse<RecommendationFeedbackResponse> clearReaction(
            @PathVariable Long searchLogId,
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestHeader(value = "X-Anonymous-Id", required = false) String anonymousId
    ) {
        return ApiResponse.ok(service.clearReaction(searchLogId, principal, anonymousId));
    }

    @PutMapping("/{searchLogId}/cooked")
    public ApiResponse<RecommendationFeedbackResponse> markCooked(
            @PathVariable Long searchLogId,
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestHeader(value = "X-Anonymous-Id", required = false) String anonymousId
    ) {
        return ApiResponse.ok(service.markCooked(searchLogId, principal, anonymousId));
    }
}
