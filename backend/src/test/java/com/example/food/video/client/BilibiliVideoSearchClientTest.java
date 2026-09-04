package com.example.food.video.client;

import com.example.food.video.BilibiliVideoProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class BilibiliVideoSearchClientTest {

    @Test
    void mapsAndSanitizesVideoResults() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        BilibiliVideoProperties properties = properties();
        BilibiliVideoSearchClient client = new BilibiliVideoSearchClient(restTemplate, new ObjectMapper(), properties);

        server.expect(once(), requestTo(properties.baseUrl() + "?search_type=video&keyword=tomato&page=1&page_size=6"))
                .andExpect(method(GET))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("""
                        {
                          "code": 0,
                          "data": {
                            "numPages": 2,
                            "result": [
                              {
                                "aid": 123,
                                "bvid": "BV1Q541167Qg",
                                "title": "<em class=\\\"keyword\\\">Tomato</em> &amp; egg",
                                "pic": "//i0.hdslb.com/bfs/archive/cover.jpg",
                                "author": "chef",
                                "duration": "01:02",
                                "pubdate": 1700000000,
                                "play": 12000,
                                "arcurl": "https://cm.bilibili.com/ad"
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        BilibiliVideoSearchClient.SearchResult result = client.search("tomato", 1, 6);

        assertThat(result.hasMore()).isTrue();
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.bvid()).isEqualTo("BV1Q541167Qg");
            assertThat(item.title()).isEqualTo("Tomato & egg");
            assertThat(item.coverUrl()).isEqualTo("https://i0.hdslb.com/bfs/archive/cover.jpg");
            assertThat(item.durationSeconds()).isEqualTo(62);
            assertThat(item.publishedAt()).isEqualTo(1700000000L);
            assertThat(item.playCount()).isEqualTo(12000L);
        });
        server.verify();
    }

    @Test
    void dropsInvalidItemsAndUntrustedCovers() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        BilibiliVideoProperties properties = properties();
        BilibiliVideoSearchClient client = new BilibiliVideoSearchClient(restTemplate, new ObjectMapper(), properties);

        server.expect(once(), requestTo(properties.baseUrl() + "?search_type=video&keyword=tomato&page=1&page_size=6"))
                .andRespond(withSuccess("""
                        {
                          "code": 0,
                          "data": {
                            "result": [
                              {"bvid": "not-a-bvid", "title": "drop"},
                              {"bvid": "BV1Q541167Qg", "title": "valid", "pic": "https://evil.example/cover.jpg"},
                              {"bvid": "BV1Q541167Q", "title": "drop"},
                              {"bvid": "BV1Q541167Qg", "title": "   "}
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        List<BilibiliVideoSearchClient.VideoCandidate> items = client.search("tomato", 1, 6).items();

        assertThat(items).singleElement().satisfies(item -> {
            assertThat(item.bvid()).isEqualTo("BV1Q541167Qg");
            assertThat(item.coverUrl()).isNull();
        });
    }

    @Test
    void doesNotRetryPreconditionFailure() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        BilibiliVideoProperties properties = properties();
        BilibiliVideoSearchClient client = new BilibiliVideoSearchClient(restTemplate, new ObjectMapper(), properties);

        server.expect(once(), requestTo(properties.baseUrl() + "?search_type=video&keyword=tomato&page=1&page_size=6"))
                .andRespond(withStatus(HttpStatus.PRECONDITION_FAILED));

        assertThatThrownBy(() -> client.search("tomato", 1, 6))
                .isInstanceOfSatisfying(BilibiliVideoSearchException.class, exception -> {
                    assertThat(exception.failureType()).isEqualTo(BilibiliVideoSearchException.FailureType.PRECONDITION_FAILED);
                    assertThat(exception.statusCode()).isEqualTo(412);
                });
        server.verify();
    }

    @Test
    void retriesTransientServerFailureOnlyOnce() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        BilibiliVideoProperties properties = properties();
        BilibiliVideoSearchClient client = new BilibiliVideoSearchClient(restTemplate, new ObjectMapper(), properties);

        server.expect(twice(), requestTo(properties.baseUrl() + "?search_type=video&keyword=tomato&page=1&page_size=6"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.search("tomato", 1, 6))
                .isInstanceOf(BilibiliVideoSearchException.class)
                .satisfies(error -> assertThat(((BilibiliVideoSearchException) error).failureType())
                        .isEqualTo(BilibiliVideoSearchException.FailureType.UPSTREAM));
        server.verify();
    }

    @Test
    void rejectsNonZeroCodeAndMalformedShape() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        BilibiliVideoProperties properties = properties();
        BilibiliVideoSearchClient client = new BilibiliVideoSearchClient(restTemplate, new ObjectMapper(), properties);

        server.expect(once(), requestTo(properties.baseUrl() + "?search_type=video&keyword=tomato&page=1&page_size=6"))
                .andRespond(withSuccess("{\"code\": -412, \"message\": \"blocked\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.search("tomato", 1, 6))
                .isInstanceOfSatisfying(BilibiliVideoSearchException.class, exception ->
                        assertThat(exception.failureType()).isEqualTo(BilibiliVideoSearchException.FailureType.UPSTREAM));

        server.reset();
        server.expect(once(), requestTo(properties.baseUrl() + "?search_type=video&keyword=tomato&page=1&page_size=6"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.search("tomato", 1, 6))
                .isInstanceOfSatisfying(BilibiliVideoSearchException.class, exception ->
                        assertThat(exception.failureType()).isEqualTo(BilibiliVideoSearchException.FailureType.INVALID_RESPONSE));
    }

    private BilibiliVideoProperties properties() {
        BilibiliVideoProperties properties = new BilibiliVideoProperties();
        properties.setBaseUrl("https://bilibili.test/search");
        return properties;
    }
}
