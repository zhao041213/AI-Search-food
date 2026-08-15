package com.example.food.review;

import com.example.food.review.dto.FinishedDishReviewRequest;
import com.example.food.review.dto.FinishedDishReviewResponse;
import com.example.food.review.dto.FinishedDishReviewResult;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.unit.DataSize;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinishedDishReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MultipartProperties multipartProperties;

    @MockBean
    private FinishedDishReviewService reviewService;

    @Test
    void anonymousVisitorCanRequestOneTimeReview() throws Exception {
        when(reviewService.create(any(), any(), isNull())).thenReturn(response(false));
        MockMultipartFile request = new MockMultipartFile(
                "request",
                "request.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(new FinishedDishReviewRequest(
                        null, "番茄炒蛋", List.of("番茄"), List.of("翻炒")
                ))
        );
        MockMultipartFile image = new MockMultipartFile(
                "image", "dish.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/ai/finished-dish-reviews").file(request).file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.saved").value(false))
                .andExpect(jsonPath("$.data.result.overallScore").value(86));
    }

    @Test
    void multipartLimitMatchesTheFiveMegabyteImageRule() {
        assertThat(multipartProperties.getMaxFileSize()).isEqualTo(DataSize.ofMegabytes(5));
        assertThat(multipartProperties.getMaxRequestSize()).isEqualTo(DataSize.ofMegabytes(6));
    }

    @Test
    void reviewHistoryRequiresUserAuthentication() throws Exception {
        mockMvc.perform(get("/api/finished-dish-reviews"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void userCanReadOwnReviewHistory() throws Exception {
        when(reviewService.list(7L, 21L, 10)).thenReturn(List.of(response(true)));
        String token = jwtService.generateToken(new AuthPrincipal(7L, "13800138000", AppRole.USER));

        mockMvc.perform(get("/api/finished-dish-reviews")
                        .header("Authorization", "Bearer " + token)
                        .param("recipeId", "21")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].saved").value(true))
                .andExpect(jsonPath("$.data[0].result.recipeTitle").value("番茄炒蛋"));

        verify(reviewService).list(7L, 21L, 10);
    }

    private FinishedDishReviewResponse response(boolean saved) {
        return new FinishedDishReviewResponse(
                saved ? 41L : null,
                saved ? 21L : null,
                new FinishedDishReviewResult(
                        "番茄炒蛋",
                        86,
                        new FinishedDishReviewResult.Dimension(88, "颜色自然"),
                        new FinishedDishReviewResult.Dimension(82, "火候合适"),
                        new FinishedDishReviewResult.Dimension(80, "摆盘整洁"),
                        "整体完成度较好",
                        List.of("食材层次清晰"),
                        List.of(),
                        "仅根据照片提供视觉烹饪建议，无法替代食品安全判断。",
                        "qwen",
                        "qwen-vl-plus"
                ),
                saved,
                null
        );
    }
}
