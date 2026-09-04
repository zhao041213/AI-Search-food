package com.example.food.video.client;

import com.example.food.video.BilibiliVideoProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class BilibiliVideoSearchClient {

    private static final String USER_AGENT = "AI-Smart-Recipe/1.0 (graduation project)";
    private static final Pattern BVID_PATTERN = Pattern.compile("BV[0-9A-Za-z]{10}");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Set<String> EXACT_COVER_HOSTS = Set.of(
            "i0.hdslb.com",
            "i1.hdslb.com",
            "i2.hdslb.com",
            "i3.hdslb.com",
            "i4.hdslb.com",
            "i5.hdslb.com",
            "i6.hdslb.com"
    );
    private static final int MAX_TITLE_LENGTH = 180;
    private static final int MAX_AUTHOR_LENGTH = 64;
    private static final int MAX_VIDEO_ID_LENGTH = 64;
    private static final int MAX_ATTEMPTS = 2;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final BilibiliVideoProperties properties;

    @Autowired
    public BilibiliVideoSearchClient(
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            BilibiliVideoProperties properties
    ) {
        this(
                restTemplateBuilder
                        .setConnectTimeout(timeoutOrDefault(properties.connectTimeout(), Duration.ofSeconds(3)))
                        .setReadTimeout(timeoutOrDefault(properties.readTimeout(), Duration.ofSeconds(5)))
                        .build(),
                objectMapper,
                properties
        );
    }

    public BilibiliVideoSearchClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            BilibiliVideoProperties properties
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public SearchResult search(String keyword, int page, int limit) {
        URI uri = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .queryParam("search_type", "video")
                .queryParam("keyword", keyword)
                .queryParam("page", page)
                .queryParam("page_size", limit)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                ResponseEntity<JsonNode> response = restTemplate.exchange(
                        uri,
                        HttpMethod.GET,
                        new HttpEntity<>(requestHeaders()),
                        JsonNode.class
                );
                return parse(response.getBody(), page, limit);
            } catch (RestClientResponseException exception) {
                if (attempt < MAX_ATTEMPTS && shouldRetry(exception)) {
                    continue;
                }
                throw failureFor(exception);
            } catch (ResourceAccessException exception) {
                if (attempt < MAX_ATTEMPTS) {
                    continue;
                }
                throw new BilibiliVideoSearchException(
                        isTimeout(exception)
                                ? BilibiliVideoSearchException.FailureType.TIMEOUT
                                : BilibiliVideoSearchException.FailureType.UPSTREAM,
                        "Bilibili 视频搜索网络请求失败",
                        null,
                        exception
                );
            } catch (RestClientException exception) {
                throw new BilibiliVideoSearchException(
                        BilibiliVideoSearchException.FailureType.UPSTREAM,
                        "Bilibili 视频搜索请求失败",
                        null,
                        exception
                );
            }
        }
        throw new BilibiliVideoSearchException(
                BilibiliVideoSearchException.FailureType.UPSTREAM,
                "Bilibili 视频搜索请求失败",
                null,
                null
        );
    }

    private SearchResult parse(JsonNode body, int page, int limit) {
        if (body == null || !body.isObject()) {
            throw invalidResponse();
        }
        JsonNode codeNode = body.get("code");
        if (codeNode == null || !codeNode.canConvertToInt()) {
            throw invalidResponse();
        }
        int code = codeNode.asInt();
        if (code != 0) {
            throw new BilibiliVideoSearchException(
                    BilibiliVideoSearchException.FailureType.UPSTREAM,
                    "Bilibili 视频搜索返回失败",
                    null,
                    null
            );
        }

        JsonNode data = body.get("data");
        JsonNode result = data == null ? null : data.get("result");
        if (data == null || !data.isObject() || result == null || !result.isArray()) {
            throw invalidResponse();
        }

        List<VideoCandidate> candidates = new ArrayList<>();
        result.forEach(item -> parseCandidate(item).ifPresent(candidates::add));
        int numPages = positiveInt(data.get("numPages"));
        boolean hasMore = numPages > 0 ? numPages > page : result.size() >= limit;
        return new SearchResult(List.copyOf(candidates), hasMore);
    }

    private java.util.Optional<VideoCandidate> parseCandidate(JsonNode item) {
        if (item == null || !item.isObject()) {
            return java.util.Optional.empty();
        }
        String bvid = plainText(item.get("bvid"), 32);
        String title = plainText(item.get("title"), MAX_TITLE_LENGTH);
        if (!isValidBvid(bvid) || title == null || title.isBlank()) {
            return java.util.Optional.empty();
        }
        String videoId = plainText(item.get("aid"), MAX_VIDEO_ID_LENGTH);
        if (videoId == null || videoId.isBlank()) {
            videoId = bvid;
        }
        return java.util.Optional.of(new VideoCandidate(
                videoId,
                bvid,
                title,
                normalizeCoverUrl(plainText(item.get("pic"), 512)),
                plainText(item.get("author"), MAX_AUTHOR_LENGTH),
                parseDurationSeconds(plainText(item.get("duration"), 32)),
                positiveLong(item.get("pubdate")),
                positiveLong(item.get("play"))
        ));
    }

    private BilibiliVideoSearchException failureFor(RestClientResponseException exception) {
        int statusCode = exception.getStatusCode().value();
        BilibiliVideoSearchException.FailureType failureType = switch (statusCode) {
            case 412 -> BilibiliVideoSearchException.FailureType.PRECONDITION_FAILED;
            case 403 -> BilibiliVideoSearchException.FailureType.FORBIDDEN;
            case 429 -> BilibiliVideoSearchException.FailureType.RATE_LIMITED;
            default -> BilibiliVideoSearchException.FailureType.UPSTREAM;
        };
        return new BilibiliVideoSearchException(
                failureType,
                "Bilibili 视频搜索返回 HTTP " + statusCode,
                statusCode,
                exception
        );
    }

    private boolean shouldRetry(RestClientResponseException exception) {
        int statusCode = exception.getStatusCode().value();
        return statusCode >= 500 && statusCode <= 599;
    }

    private BilibiliVideoSearchException invalidResponse() {
        return new BilibiliVideoSearchException(
                BilibiliVideoSearchException.FailureType.INVALID_RESPONSE,
                "Bilibili 视频搜索返回结构异常",
                null,
                null
        );
    }

    private HttpHeaders requestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
        return headers;
    }

    private String normalizeCoverUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.startsWith("//") ? "https:" + value : value;
        try {
            URI uri = URI.create(normalized);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !isAllowedCoverHost(host)) {
                return null;
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean isAllowedCoverHost(String host) {
        if (host == null) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return EXACT_COVER_HOSTS.contains(normalized)
                || normalized.endsWith(".hdslb.com")
                || normalized.endsWith(".bilivideo.com");
    }

    private String plainText(JsonNode node, int maxLength) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        if (value == null) {
            return null;
        }
        String cleaned = HtmlUtils.htmlUnescape(value);
        cleaned = HTML_TAG_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = HtmlUtils.htmlUnescape(cleaned).replaceAll("\\s+", " ").trim();
        return limitText(cleaned, maxLength);
    }

    private Integer parseDurationSeconds(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.trim().split(":");
        if (parts.length < 2 || parts.length > 3) {
            return null;
        }
        try {
            int seconds = Integer.parseInt(parts[parts.length - 1]);
            int minutes = Integer.parseInt(parts[parts.length - 2]);
            int hours = parts.length == 3 ? Integer.parseInt(parts[0]) : 0;
            if (seconds < 0 || seconds > 59 || minutes < 0 || minutes > 59 || hours < 0) {
                return null;
            }
            long total = hours * 3600L + minutes * 60L + seconds;
            return total > Integer.MAX_VALUE ? null : (int) total;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private int positiveInt(JsonNode node) {
        if (node == null || !node.canConvertToInt()) {
            return 0;
        }
        int value = node.asInt();
        return value > 0 ? value : 0;
    }

    private Long positiveLong(JsonNode node) {
        if (node == null || !node.canConvertToLong()) {
            return null;
        }
        long value = node.asLong();
        return value > 0 ? value : null;
    }

    private boolean isValidBvid(String bvid) {
        return bvid != null && BVID_PATTERN.matcher(bvid).matches();
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private boolean isTimeout(Throwable exception) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current instanceof java.net.SocketTimeoutException
                    || current instanceof java.net.SocketException && current.getMessage() != null
                    && current.getMessage().toLowerCase(Locale.ROOT).contains("timed out")) {
                return true;
            }
        }
        return false;
    }

    private static Duration timeoutOrDefault(Duration value, Duration fallback) {
        return value == null || value.isNegative() || value.isZero() ? fallback : value;
    }

    public record SearchResult(List<VideoCandidate> items, boolean hasMore) {
        public SearchResult {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record VideoCandidate(
            String videoId,
            String bvid,
            String title,
            String coverUrl,
            String author,
            Integer durationSeconds,
            Long publishedAt,
            Long playCount
    ) {
    }
}
