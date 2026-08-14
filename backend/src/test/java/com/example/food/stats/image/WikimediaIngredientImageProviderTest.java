package com.example.food.stats.image;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WikimediaIngredientImageProviderTest {

    @Test
    void selectsAnAllowedImageCandidateFromWikimediaSearchResponse() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        WikimediaIngredientImageProvider provider = new WikimediaIngredientImageProvider(restTemplate);

        server.expect(requestTo(startsWith("https://commons.wikimedia.org/w/api.php")))
                .andExpect(header(HttpHeaders.USER_AGENT, "AI-Smart-Recipe/1.0 (graduation project)"))
                .andRespond(withSuccess("""
                        {
                          "query": {
                            "pages": {
                              "12": {
                                "imageinfo": [
                                  {
                                    "mime": "image/jpeg",
                                    "thumburl": "https://upload.wikimedia.org/wikipedia/commons/thumb/example.jpg",
                                    "descriptionurl": "https://commons.wikimedia.org/wiki/File:Example.jpg"
                                  }
                                ]
                              }
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        List<IngredientImageCandidate> candidates = provider.findCandidates("番茄");

        assertThat(candidates).hasSize(1);
        IngredientImageCandidate candidate = candidates.get(0);
        assertThat(candidate.imageUrl()).startsWith("https://upload.wikimedia.org/");
        assertThat(candidate.sourceUrl()).isEqualTo("https://commons.wikimedia.org/wiki/File:Example.jpg");
        assertThat(candidate.contentType()).isEqualTo("image/jpeg");
        server.verify();
    }
}
