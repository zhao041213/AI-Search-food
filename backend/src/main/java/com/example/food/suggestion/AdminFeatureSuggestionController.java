package com.example.food.suggestion;

import com.example.food.common.ApiResponse;
import com.example.food.security.AuthPrincipal;
import com.example.food.suggestion.dto.FeatureSuggestionAdminPageResponse;
import com.example.food.suggestion.dto.FeatureSuggestionAdminResponse;
import com.example.food.suggestion.dto.FeatureSuggestionAdminUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/admin/feature-suggestions")
public class AdminFeatureSuggestionController {
    private final FeatureSuggestionService service;

    public AdminFeatureSuggestionController(FeatureSuggestionService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<FeatureSuggestionAdminPageResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(service.listAdmin(keyword, type, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<FeatureSuggestionAdminResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detailAdmin(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<FeatureSuggestionAdminResponse> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody FeatureSuggestionAdminUpdateRequest request
    ) {
        return ApiResponse.ok(service.updateAdmin(principal.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id
    ) {
        service.deleteAdmin(principal.id(), id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/screenshot")
    public ResponseEntity<byte[]> screenshot(@PathVariable Long id) {
        FeatureSuggestionFileStorage.StoredFileInfo image = service.image(id, null, true);
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noStore());
        headers.setContentType(MediaType.parseMediaType(image.contentType()));
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(image.originalName(), StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(image.bytes(), headers, HttpStatus.OK);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noStore());
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("feature-suggestions.csv", StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(service.exportCsv(keyword, type, status), headers, HttpStatus.OK);
    }
}
