package com.example.food.video.dto;

public record VideoSearchItem(
        String videoId,
        String bvid,
        String title,
        String coverUrl,
        String source,
        String author,
        Integer durationSeconds,
        Long publishedAt,
        String targetUrl,
        Long playCount
) {
}
