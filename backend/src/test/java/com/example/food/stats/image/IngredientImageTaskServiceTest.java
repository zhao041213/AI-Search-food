package com.example.food.stats.image;

import com.example.food.ai.qwen.QwenVisionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientImageTaskServiceTest {

    @Mock
    private IngredientImageMapper imageMapper;

    @Mock
    private IngredientImageProvider imageProvider;

    @Mock
    private QwenVisionClient qwenVisionClient;

    private IngredientImageTaskService service;

    @BeforeEach
    void setUp() {
        service = new IngredientImageTaskService(imageMapper, imageProvider, qwenVisionClient);
    }

    @Test
    void storesImageOnlyAfterQwenVisualVerificationPasses() {
        IngredientImageCandidate candidate = new IngredientImageCandidate(
                "https://upload.wikimedia.org/example.jpg",
                "https://commons.wikimedia.org/wiki/File:example.jpg",
                "image/jpeg",
                "wikimedia-commons"
        );
        IngredientImage image = new IngredientImage();
        image.setId(8L);
        when(imageProvider.findCandidates("番茄")).thenReturn(List.of(candidate));
        when(imageProvider.download(candidate)).thenReturn(new IngredientImageContent(new byte[]{1, 2, 3}, "image/jpeg"));
        when(qwenVisionClient.verifyIngredientImage(eq("image/jpeg"), any(byte[].class), eq("番茄")))
                .thenReturn(new QwenVisionClient.IngredientImageVerification(true, 0.91d, "番茄"));
        when(imageMapper.selectById(8L)).thenReturn(image);

        service.enqueue(8L, "番茄");

        ArgumentCaptor<IngredientImage> captor = ArgumentCaptor.forClass(IngredientImage.class);
        verify(imageMapper).updateById(captor.capture());
        IngredientImage saved = captor.getValue();
        assertThat(saved.getVerificationStatus()).isEqualTo("READY");
        assertThat(saved.getImageData()).containsExactly(1, 2, 3);
        assertThat(saved.getSourceUrl()).isEqualTo(candidate.sourceUrl());
        assertThat(saved.getVerificationScore()).isEqualByComparingTo("0.91");
    }

    @Test
    void marksCacheEntryFailedWhenVisualVerificationDoesNotMatch() {
        IngredientImageCandidate candidate = new IngredientImageCandidate(
                "https://upload.wikimedia.org/example.jpg",
                "https://commons.wikimedia.org/wiki/File:example.jpg",
                "image/jpeg",
                "wikimedia-commons"
        );
        IngredientImage image = new IngredientImage();
        image.setId(9L);
        when(imageProvider.findCandidates("土豆")).thenReturn(List.of(candidate));
        when(imageProvider.download(candidate)).thenReturn(new IngredientImageContent(new byte[]{1, 2}, "image/jpeg"));
        when(qwenVisionClient.verifyIngredientImage(eq("image/jpeg"), any(byte[].class), eq("土豆")))
                .thenReturn(new QwenVisionClient.IngredientImageVerification(false, 0.96d, "不是土豆"));
        when(imageMapper.selectById(9L)).thenReturn(image);

        service.enqueue(9L, "土豆");

        verify(imageMapper).updateById(image);
        assertThat(image.getVerificationStatus()).isEqualTo("FAILED");
        assertThat(image.getFailureReason()).isEqualTo("所有候选图片均未通过千问视觉校验");
    }

    @Test
    void triesTheNextCandidateWhenTheFirstImageDoesNotMatch() {
        IngredientImageCandidate firstCandidate = new IngredientImageCandidate(
                "https://upload.wikimedia.org/first.jpg",
                "https://commons.wikimedia.org/wiki/File:first.jpg",
                "image/jpeg",
                "wikimedia-commons"
        );
        IngredientImageCandidate secondCandidate = new IngredientImageCandidate(
                "https://upload.wikimedia.org/second.jpg",
                "https://commons.wikimedia.org/wiki/File:second.jpg",
                "image/jpeg",
                "wikimedia-commons"
        );
        IngredientImage image = new IngredientImage();
        image.setId(10L);
        when(imageProvider.findCandidates("茄子")).thenReturn(List.of(firstCandidate, secondCandidate));
        when(imageProvider.download(firstCandidate))
                .thenReturn(new IngredientImageContent(new byte[]{1}, "image/jpeg"));
        when(imageProvider.download(secondCandidate))
                .thenReturn(new IngredientImageContent(new byte[]{2}, "image/jpeg"));
        when(qwenVisionClient.verifyIngredientImage(eq("image/jpeg"), any(byte[].class), eq("茄子")))
                .thenReturn(
                        new QwenVisionClient.IngredientImageVerification(false, 0.95d, "不是茄子"),
                        new QwenVisionClient.IngredientImageVerification(true, 0.93d, "茄子")
                );
        when(imageMapper.selectById(10L)).thenReturn(image);

        service.enqueue(10L, "茄子");

        verify(imageMapper).updateById(image);
        assertThat(image.getVerificationStatus()).isEqualTo("READY");
        assertThat(image.getImageData()).containsExactly(2);
        assertThat(image.getSourceUrl()).isEqualTo(secondCandidate.sourceUrl());
    }
}
