package com.example.food.video;

import com.example.food.common.ApiResponse;
import com.example.food.security.AuthPrincipal;
import com.example.food.video.dto.VideoSearchResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/videos")
public class VideoSearchController {

    private final VideoSearchService service;

    public VideoSearchController(VideoSearchService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public ApiResponse<VideoSearchResponse> search(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam String recipeTitle,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.ok(service.search(
                principal == null ? null : principal.id(),
                principal,
                recipeTitle,
                keyword,
                page,
                limit
        ));
    }
}
