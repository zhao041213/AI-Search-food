package com.example.food.video;

import com.example.food.admin.error.AdminErrorLogService;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.video.client.BilibiliVideoSearchClient;
import com.example.food.video.client.BilibiliVideoSearchException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoSearchServiceTest {

    @Mock
    private BilibiliVideoSearchClient client;

    @Mock
    private AdminErrorLogService errorLogService;

    @Test
    void combinesQueryMapsSafeResponseAndProvidesFallback() {
        BilibiliVideoProperties properties = properties();
        VideoSearchService service = service(properties, Clock.systemUTC());
        AuthPrincipal principal = new AuthPrincipal(7L, "user", AppRole.USER);
        when(client.search("Tomato Egg quick", 1, 6)).thenReturn(new BilibiliVideoSearchClient.SearchResult(
                List.of(new BilibiliVideoSearchClient.VideoCandidate(
                        "123", "BV1Q541167Qg", "Tomato Egg", "https://i0.hdslb.com/cover.jpg",
                        "chef", 62, 1700000000L, 12L
                )),
                true
        ));

        var response = service.search(7L, principal, " Tomato Egg ", "quick", null, null);

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.source()).isEqualTo(VideoSearchService.SOURCE_BILIBILI);
            assertThat(item.targetUrl()).isEqualTo("https://www.bilibili.com/video/BV1Q541167Qg");
            assertThat(item.coverUrl()).isEqualTo("https://i0.hdslb.com/cover.jpg");
        });
        assertThat(response.fallbackSearchUrl()).startsWith("https://search.bilibili.com/all?");
        assertThat(response.hasMore()).isTrue();
        verify(client).search("Tomato Egg quick", 1, 6);
    }

    @Test
    void cachesResultsAndExpiresThem() {
        BilibiliVideoProperties properties = properties();
        properties.setCacheTtl(Duration.ofMinutes(1));
        MutableClock clock = new MutableClock();
        VideoSearchService service = service(properties, clock);
        when(client.search("Tomato", 1, 6)).thenReturn(new BilibiliVideoSearchClient.SearchResult(List.of(), false));

        var first = service.search(7L, null, "Tomato", null, 1, 6);
        var cached = service.search(7L, null, "Tomato", null, 1, 6);
        clock.advance(Duration.ofMinutes(2));
        var afterExpiry = service.search(7L, null, "Tomato", null, 1, 6);

        assertThat(first.cached()).isFalse();
        assertThat(cached.cached()).isTrue();
        assertThat(afterExpiry.cached()).isFalse();
        verify(client, org.mockito.Mockito.times(2)).search("Tomato", 1, 6);
    }

    @Test
    void disabledFeatureReturnsFallbackWithoutCallingUpstream() {
        BilibiliVideoProperties properties = properties();
        properties.setEnabled(false);
        VideoSearchService service = service(properties, Clock.systemUTC());

        var response = service.search(7L, null, "Tomato", "recipe", 1, 6);

        assertThat(response.items()).isEmpty();
        assertThat(response.degraded()).isTrue();
        assertThat(response.message()).isEqualTo("视频搜索功能暂未开启");
        assertThat(response.fallbackSearchUrl()).contains("Tomato");
        verify(client, never()).search(any(), any(Integer.class), any(Integer.class));
    }

    @Test
    void rateLimitKeepsBlockingTheSameUserAfterFirstRequest() {
        BilibiliVideoProperties properties = properties();
        properties.setCacheTtl(Duration.ZERO);
        properties.setRateLimitMaxRequests(1);
        VideoSearchService service = service(properties, Clock.systemUTC());
        when(client.search("Tomato", 1, 6)).thenReturn(new BilibiliVideoSearchClient.SearchResult(List.of(), false));

        service.search(7L, null, "Tomato", null, 1, 6);

        assertThatThrownBy(() -> service.search(7L, null, "Tomato", null, 1, 6))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value()).isEqualTo(429));
        assertThatThrownBy(() -> service.search(7L, null, "Tomato", null, 1, 6))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void upstreamFailureReturnsStableDegradedResponseAndRecordsToolFailure() {
        BilibiliVideoProperties properties = properties();
        VideoSearchService service = service(properties, Clock.systemUTC());
        BilibiliVideoSearchException failure = new BilibiliVideoSearchException(
                BilibiliVideoSearchException.FailureType.RATE_LIMITED,
                "upstream secret=do-not-return",
                429,
                null
        );
        when(client.search("Tomato", 1, 6)).thenThrow(failure);

        var response = service.search(7L, new AuthPrincipal(7L, "user", AppRole.USER), "Tomato", null, 1, 6);

        assertThat(response.degraded()).isTrue();
        assertThat(response.items()).isEmpty();
        assertThat(response.message()).isEqualTo(VideoSearchService.DEGRADE_MESSAGE);
        assertThat(response.message()).doesNotContain("secret");
        verify(errorLogService).recordFailure(
                eq(AdminErrorLogService.TOOL),
                eq("BilibiliVideoSearchClient"),
                any(String.class),
                eq(failure),
                any(AuthPrincipal.class),
                eq("GET"),
                eq("/api/videos/search"),
                eq(429),
                eq(null)
        );
    }

    @Test
    void rejectsInvalidPaginationAndOverlongInputs() {
        VideoSearchService service = service(properties(), Clock.systemUTC());

        assertThatThrownBy(() -> service.search(7L, null, "Tomato", null, 0, 6))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search(7L, null, "Tomato", null, 1, 7))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search(7L, null, "Tomato", "x".repeat(97), 1, 6))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private VideoSearchService service(BilibiliVideoProperties properties, Clock clock) {
        return new VideoSearchService(client, properties, errorLogService, clock);
    }

    private BilibiliVideoProperties properties() {
        BilibiliVideoProperties properties = new BilibiliVideoProperties();
        properties.setMaxResults(6);
        properties.setRateLimitWindow(Duration.ofMinutes(1));
        properties.setRateLimitMaxRequests(12);
        return properties;
    }

    private static final class MutableClock extends Clock {
        private Instant current = Instant.parse("2026-09-04T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }
    }
}
