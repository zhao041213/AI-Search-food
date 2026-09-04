package com.example.food.video.dto;

import java.util.List;

public record VideoSearchResponse(
        List<VideoSearchItem> items,
        int page,
        boolean hasMore,
        boolean cached,
        boolean degraded,
        String fallbackSearchUrl,
        String message
) {
    public VideoSearchResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
