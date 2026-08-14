package com.example.food.stats.image;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class WikimediaIngredientImageProvider implements IngredientImageProvider {

    private static final String API_ENDPOINT = "https://commons.wikimedia.org/w/api.php";
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "upload.wikimedia.org",
            "commons.wikimedia.org"
    );
    private static final Map<String, String> SEARCH_TERMS = Map.ofEntries(
            Map.entry("番茄", "tomato"),
            Map.entry("鸡蛋", "chicken egg"),
            Map.entry("土豆", "potato"),
            Map.entry("胡萝卜", "carrot"),
            Map.entry("西兰花", "broccoli"),
            Map.entry("豆腐", "tofu"),
            Map.entry("青椒", "green bell pepper"),
            Map.entry("洋葱", "onion"),
            Map.entry("大蒜", "garlic"),
            Map.entry("生姜", "ginger"),
            Map.entry("猪肉", "pork"),
            Map.entry("牛肉", "beef"),
            Map.entry("鸡肉", "chicken meat"),
            Map.entry("虾", "shrimp"),
            Map.entry("茄子", "intitle:eggplant"),
            Map.entry("黄瓜", "cucumber"),
            Map.entry("白菜", "Chinese cabbage"),
            Map.entry("菠菜", "spinach"),
            Map.entry("玉米", "corn"),
            Map.entry("蘑菇", "mushroom"),
            Map.entry("香菇", "shiitake mushroom"),
            Map.entry("米饭", "cooked rice"),
            Map.entry("面条", "noodles")
    );

    private final RestTemplate restTemplate;

    @Autowired
    public WikimediaIngredientImageProvider(RestTemplateBuilder restTemplateBuilder) {
        this(restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(20))
                .build());
    }

    WikimediaIngredientImageProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<IngredientImageCandidate> findCandidates(String canonicalName) {
        String searchTerm = SEARCH_TERMS.getOrDefault(canonicalName, canonicalName + " food");
        URI uri = UriComponentsBuilder.fromHttpUrl(API_ENDPOINT)
                .queryParam("action", "query")
                .queryParam("generator", "search")
                .queryParam("gsrsearch", searchTerm + " filetype:bitmap")
                .queryParam("gsrnamespace", 6)
                .queryParam("gsrlimit", 5)
                .queryParam("prop", "imageinfo")
                .queryParam("iiprop", "url|mime|size")
                .queryParam("iiurlwidth", 800)
                .queryParam("format", "json")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    new HttpEntity<>(requestHeaders()),
                    JsonNode.class
            );
            return parseCandidates(response.getBody());
        } catch (RestClientException exception) {
            throw new IllegalStateException("图片候选搜索失败", exception);
        }
    }

    @Override
    public IngredientImageContent download(IngredientImageCandidate candidate) {
        if (candidate == null || !isAllowedImageUrl(candidate.imageUrl())) {
            throw new IllegalArgumentException("图片地址不在允许的 Wikimedia 域名内");
        }
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    URI.create(candidate.imageUrl()),
                    HttpMethod.GET,
                    new HttpEntity<>(requestHeaders()),
                    byte[].class
            );
            byte[] bytes = response.getBody();
            if (bytes == null || bytes.length == 0) {
                throw new IllegalStateException("候选图片内容为空");
            }
            if (bytes.length > MAX_IMAGE_SIZE_BYTES) {
                throw new IllegalStateException("候选图片超过 5MB");
            }
            String contentType = normalizeContentType(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
            if (!SUPPORTED_CONTENT_TYPES.contains(contentType)) {
                contentType = normalizeContentType(candidate.contentType());
            }
            if (!SUPPORTED_CONTENT_TYPES.contains(contentType)) {
                throw new IllegalStateException("候选图片格式不受支持");
            }
            return new IngredientImageContent(bytes, contentType);
        } catch (RestClientException exception) {
            throw new IllegalStateException("候选图片下载失败", exception);
        }
    }

    private List<IngredientImageCandidate> parseCandidates(JsonNode body) {
        JsonNode pages = body == null ? null : body.path("query").path("pages");
        if (pages == null || pages.isMissingNode() || pages.isNull()) {
            return List.of();
        }
        List<IngredientImageCandidate> candidates = new ArrayList<>();
        Iterator<JsonNode> pageIterator = pages.elements();
        while (pageIterator.hasNext()) {
            JsonNode info = pageIterator.next().path("imageinfo");
            if (!info.isArray() || info.isEmpty()) {
                continue;
            }
            JsonNode imageInfo = info.get(0);
            String contentType = normalizeContentType(imageInfo.path("mime").asText(null));
            String imageUrl = firstText(imageInfo, "thumburl", "url");
            String sourceUrl = imageInfo.path("descriptionurl").asText(imageUrl);
            if (SUPPORTED_CONTENT_TYPES.contains(contentType) && isAllowedImageUrl(imageUrl)) {
                candidates.add(new IngredientImageCandidate(
                        imageUrl,
                        sourceUrl,
                        contentType,
                        "wikimedia-commons"
                ));
            }
        }
        return List.copyOf(candidates);
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText(null);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private HttpHeaders requestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "AI-Smart-Recipe/1.0 (graduation project)");
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.IMAGE_JPEG,
                MediaType.IMAGE_PNG, MediaType.parseMediaType("image/webp")));
        return headers;
    }

    private boolean isAllowedImageUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && ALLOWED_HOSTS.contains(uri.getHost().toLowerCase());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String normalizeContentType(String value) {
        if (value == null) {
            return "";
        }
        int separator = value.indexOf(';');
        return (separator >= 0 ? value.substring(0, separator) : value).trim().toLowerCase();
    }
}
