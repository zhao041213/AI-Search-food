package com.example.food.video;

import com.example.food.admin.error.AdminErrorLogService;
import com.example.food.security.AuthPrincipal;
import com.example.food.video.client.BilibiliVideoSearchClient;
import com.example.food.video.client.BilibiliVideoSearchException;
import com.example.food.video.dto.VideoSearchItem;
import com.example.food.video.dto.VideoSearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VideoSearchService {

    public static final String SOURCE_BILIBILI = "BILIBILI";
    public static final String DEGRADE_MESSAGE = "视频服务暂时不可用，可前往B站搜索";
    private static final String FALLBACK_SEARCH_BASE_URL = "https://search.bilibili.com/all";
    private static final Logger log = LoggerFactory.getLogger(VideoSearchService.class);
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_LIMIT = 6;
    private static final int MAX_CONFIGURED_RESULTS = 12;
    private static final int MAX_PAGE = 1000;
    private static final int MAX_INPUT_LENGTH = 96;
    private static final int MAX_COMBINED_QUERY_LENGTH = 180;
    private static final int MAX_RATE_LIMITERS = 4096;

    private final BilibiliVideoSearchClient client;
    private final BilibiliVideoProperties properties;
    private final AdminErrorLogService errorLogService;
    private final Clock clock;
    private final Map<CacheKey, CacheEntry> cache;
    private final Map<Long, RateWindow> rateWindows = new ConcurrentHashMap<>();

    @Autowired
    public VideoSearchService(
            BilibiliVideoSearchClient client,
            BilibiliVideoProperties properties,
            AdminErrorLogService errorLogService
    ) {
        this(client, properties, errorLogService, Clock.system(ZoneId.of("Asia/Shanghai")));
    }

    VideoSearchService(
            BilibiliVideoSearchClient client,
            BilibiliVideoProperties properties,
            AdminErrorLogService errorLogService,
            Clock clock
    ) {
        this.client = client;
        this.properties = properties;
        this.errorLogService = errorLogService;
        this.clock = clock;
        int maxEntries = properties.cacheMaxEntries() > 0 ? properties.cacheMaxEntries() : 256;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, CacheEntry> eldest) {
                return size() > maxEntries;
            }
        });
    }

    public VideoSearchResponse search(
            Long userId,
            AuthPrincipal principal,
            String recipeTitle,
            String keyword,
            Integer page,
            Integer limit
    ) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后搜索烹饪视频");
        }
        int safePage = normalizePage(page);
        int safeLimit = normalizeLimit(limit);
        String normalizedTitle = normalizeRequired(recipeTitle, "菜谱名称不能为空");
        String normalizedKeyword = normalizeOptional(keyword);
        if (normalizedKeyword != null && normalizedKeyword.length() > MAX_INPUT_LENGTH) {
            throw new IllegalArgumentException("搜索关键词过长，请缩短后重试");
        }
        String searchTerm = combineSearchTerm(normalizedTitle, normalizedKeyword);
        String fallbackSearchUrl = fallbackSearchUrl(searchTerm);

        if (!properties.enabled()) {
            return degradedResponse(safePage, fallbackSearchUrl, "视频搜索功能暂未开启");
        }
        if (!allowRequest(userId)) {
            logSearch(searchTerm, safePage, 0, 0, false, true, "rate_limited");
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "视频请求过于频繁，请稍后再试，可前往B站搜索"
            );
        }

        CacheKey cacheKey = new CacheKey(searchTerm, safePage, safeLimit);
        VideoSearchResponse cachedResponse = getCached(cacheKey);
        if (cachedResponse != null) {
            logSearch(searchTerm, safePage, cachedResponse.items().size(), 0, true, cachedResponse.degraded(), "cache_hit");
            return withCachedFlag(cachedResponse);
        }

        long startedAt = System.nanoTime();
        try {
            BilibiliVideoSearchClient.SearchResult result = client.search(searchTerm, safePage, safeLimit);
            if (result == null) {
                throw new BilibiliVideoSearchException(
                        BilibiliVideoSearchException.FailureType.INVALID_RESPONSE,
                        "Bilibili 视频搜索结果为空",
                        null,
                        null
                );
            }
            List<VideoSearchItem> items = result.items().stream()
                    .filter(this::isUsableCandidate)
                    .map(this::toResponseItem)
                    .toList();
            VideoSearchResponse response = new VideoSearchResponse(
                    items,
                    safePage,
                    result.hasMore(),
                    false,
                    false,
                    fallbackSearchUrl,
                    null
            );
            putCached(cacheKey, response);
            logSearch(searchTerm, safePage, items.size(), elapsedMillis(startedAt), false, false, "success");
            return response;
        } catch (BilibiliVideoSearchException exception) {
            long elapsed = elapsedMillis(startedAt);
            recordExternalFailure(exception, principal);
            logSearch(searchTerm, safePage, 0, elapsed, false, true, exception.failureType().name().toLowerCase(Locale.ROOT));
            VideoSearchResponse response = degradedResponse(safePage, fallbackSearchUrl, DEGRADE_MESSAGE);
            putCached(cacheKey, response);
            return response;
        } catch (RuntimeException exception) {
            long elapsed = elapsedMillis(startedAt);
            recordExternalFailure(exception, principal);
            logSearch(searchTerm, safePage, 0, elapsed, false, true, "unexpected_failure");
            VideoSearchResponse response = degradedResponse(safePage, fallbackSearchUrl, DEGRADE_MESSAGE);
            putCached(cacheKey, response);
            return response;
        }
    }

    private VideoSearchResponse getCached(CacheKey key) {
        Instant now = clock.instant();
        synchronized (cache) {
            CacheEntry entry = cache.get(key);
            if (entry == null) {
                return null;
            }
            if (!entry.expiresAt().isAfter(now)) {
                cache.remove(key);
                return null;
            }
            return entry.response();
        }
    }

    private void putCached(CacheKey key, VideoSearchResponse response) {
        Duration ttl = properties.cacheTtl();
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }
        synchronized (cache) {
            cache.put(key, new CacheEntry(response, clock.instant().plus(ttl)));
        }
    }

    private boolean allowRequest(Long userId) {
        int maxRequests = properties.rateLimitMaxRequests() > 0 ? properties.rateLimitMaxRequests() : 12;
        Duration windowDuration = properties.rateLimitWindow();
        if (windowDuration == null || windowDuration.isNegative() || windowDuration.isZero()) {
            windowDuration = Duration.ofMinutes(1);
        }
        Instant now = clock.instant();
        if (!rateWindows.containsKey(userId) && rateWindows.size() >= MAX_RATE_LIMITERS) {
            rateWindows.keySet().stream()
                    .filter(existingUserId -> !existingUserId.equals(userId))
                    .findFirst()
                    .ifPresent(rateWindows::remove);
        }
        RateWindow window = rateWindows.computeIfAbsent(userId, ignored -> new RateWindow(now));
        synchronized (window) {
            if (!now.isBefore(window.startedAt.plus(windowDuration))) {
                window.startedAt = now;
                window.count = 0;
            }
            window.count += 1;
            if (window.count <= maxRequests) {
                return true;
            }
        }
        return false;
    }

    private VideoSearchResponse degradedResponse(int page, String fallbackSearchUrl, String message) {
        return new VideoSearchResponse(
                List.of(),
                page,
                false,
                false,
                true,
                fallbackSearchUrl,
                message
        );
    }

    private VideoSearchResponse withCachedFlag(VideoSearchResponse response) {
        return new VideoSearchResponse(
                response.items(),
                response.page(),
                response.hasMore(),
                true,
                response.degraded(),
                response.fallbackSearchUrl(),
                response.message()
        );
    }

    private boolean isUsableCandidate(BilibiliVideoSearchClient.VideoCandidate candidate) {
        return candidate != null
                && StringUtils.hasText(candidate.bvid())
                && StringUtils.hasText(candidate.title());
    }

    private VideoSearchItem toResponseItem(BilibiliVideoSearchClient.VideoCandidate candidate) {
        return new VideoSearchItem(
                candidate.videoId(),
                candidate.bvid(),
                candidate.title(),
                candidate.coverUrl(),
                SOURCE_BILIBILI,
                candidate.author(),
                candidate.durationSeconds(),
                candidate.publishedAt(),
                "https://www.bilibili.com/video/" + candidate.bvid(),
                candidate.playCount()
        );
    }

    private void recordExternalFailure(Throwable exception, AuthPrincipal principal) {
        if (errorLogService == null) {
            return;
        }
        Integer statusCode = exception instanceof BilibiliVideoSearchException searchException
                ? searchException.statusCode()
                : null;
        errorLogService.recordFailure(
                AdminErrorLogService.TOOL,
                "BilibiliVideoSearchClient",
                "Bilibili 视频搜索已降级",
                exception,
                principal,
                "GET",
                "/api/videos/search",
                statusCode,
                null
        );
    }

    private void logSearch(
            String searchTerm,
            int page,
            int resultCount,
            long durationMillis,
            boolean cached,
            boolean degraded,
            String outcome
    ) {
        log.info(
                "bilibili_video_search source=BILIBILI keywordHash={} page={} resultCount={} durationMs={} cached={} degraded={} outcome={}",
                hashKeyword(searchTerm),
                page,
                resultCount,
                durationMillis,
                cached,
                degraded,
                outcome
        );
    }

    private String hashKeyword(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(16);
            for (int index = 0; index < 8 && index < digest.length; index++) {
                result.append(String.format("%02x", digest[index]));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            return "unavailable";
        }
    }

    private String combineSearchTerm(String title, String keyword) {
        Set<String> parts = new LinkedHashSet<>();
        parts.add(title);
        if (StringUtils.hasText(keyword) && parts.stream().noneMatch(part -> part.equalsIgnoreCase(keyword))) {
            parts.add(keyword);
        }
        String combined = String.join(" ", parts);
        return limitText(combined, MAX_COMBINED_QUERY_LENGTH);
    }

    private String fallbackSearchUrl(String searchTerm) {
        return UriComponentsBuilder.fromUriString(FALLBACK_SEARCH_BASE_URL)
                .queryParam("keyword", searchTerm)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }

    private int normalizePage(Integer page) {
        int value = page == null ? DEFAULT_PAGE : page;
        if (value < 1 || value > MAX_PAGE) {
            throw new IllegalArgumentException("视频页码必须在1至" + MAX_PAGE + "之间");
        }
        return value;
    }

    private int normalizeLimit(Integer limit) {
        int configuredMax = properties.maxResults() > 0
                ? Math.min(properties.maxResults(), MAX_CONFIGURED_RESULTS)
                : DEFAULT_LIMIT;
        int value = limit == null ? Math.min(DEFAULT_LIMIT, configuredMax) : limit;
        if (value < 1 || value > configuredMax) {
            throw new IllegalArgumentException("视频数量必须在1至" + configuredMax + "之间");
        }
        return value;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException(message);
        }
        if (normalized.length() > MAX_INPUT_LENGTH) {
            throw new IllegalArgumentException("搜索关键词过长，请缩短后重试");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private record CacheKey(String searchTerm, int page, int limit) {
    }

    private record CacheEntry(VideoSearchResponse response, Instant expiresAt) {
    }

    private static final class RateWindow {
        private Instant startedAt;
        private int count;

        private RateWindow(Instant startedAt) {
            this.startedAt = startedAt;
        }
    }
}
