package com.example.food.suggestion;

import com.example.food.common.ApiResponse;
import com.example.food.security.AuthPrincipal;
import com.example.food.suggestion.dto.FeatureSuggestionAppendRequest;
import com.example.food.suggestion.dto.FeatureSuggestionCreateRequest;
import com.example.food.suggestion.dto.FeatureSuggestionPageResponse;
import com.example.food.suggestion.dto.FeatureSuggestionResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/feature-suggestions")
public class FeatureSuggestionController {
    private final FeatureSuggestionService service;

    public FeatureSuggestionController(FeatureSuggestionService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FeatureSuggestionResponse> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestPart("request") FeatureSuggestionCreateRequest request,
            @RequestPart(value = "screenshot", required = false) MultipartFile screenshot
    ) {
        return ApiResponse.ok(service.create(userId(principal), request, screenshot));
    }

    @GetMapping
    public ApiResponse<FeatureSuggestionPageResponse> list(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(service.listMine(userId(principal), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<FeatureSuggestionResponse> detail(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id
    ) {
        return ApiResponse.ok(service.detailMine(userId(principal), id));
    }

    @PostMapping("/{id}/additions")
    public ApiResponse<FeatureSuggestionResponse> append(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id,
            @Valid @org.springframework.web.bind.annotation.RequestBody FeatureSuggestionAppendRequest request
    ) {
        return ApiResponse.ok(service.appendMine(userId(principal), id, request));
    }

    @GetMapping("/{id}/screenshot")
    public ResponseEntity<byte[]> screenshot(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id
    ) {
        FeatureSuggestionFileStorage.StoredFileInfo image = service.image(id, userId(principal), false);
        return imageResponse(image);
    }

    private Long userId(AuthPrincipal principal) {
        if (principal == null || principal.id() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return principal.id();
    }

    private ResponseEntity<byte[]> imageResponse(FeatureSuggestionFileStorage.StoredFileInfo image) {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noStore());
        headers.setContentType(MediaType.parseMediaType(image.contentType()));
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(image.originalName(), StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(image.bytes(), headers, HttpStatus.OK);
    }
}
