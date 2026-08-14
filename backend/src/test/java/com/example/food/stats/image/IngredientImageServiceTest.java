package com.example.food.stats.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientImageServiceTest {

    @Mock
    private IngredientImageMapper imageMapper;

    @Mock
    private IngredientImageTaskService taskService;

    private IngredientImageService service;

    @BeforeEach
    void setUp() {
        service = new IngredientImageService(imageMapper, taskService, true);
    }

    @Test
    void createsOnePendingCacheEntryAndQueuesOneTaskPerCanonicalIngredient() {
        doAnswer(invocation -> {
            IngredientImage image = invocation.getArgument(0);
            image.setId(61L);
            return 1;
        }).when(imageMapper).insert(any(IngredientImage.class));

        service.ensureQueued(List.of("番茄", "番茄", "  "));

        ArgumentCaptor<IngredientImage> captor = ArgumentCaptor.forClass(IngredientImage.class);
        verify(imageMapper).insert(captor.capture());
        assertThat(captor.getValue().getCanonicalName()).isEqualTo("番茄");
        assertThat(captor.getValue().getVerificationStatus()).isEqualTo("PENDING");
        verify(taskService).enqueue(61L, "番茄");
    }

    @Test
    void keepsVerifiedImageWithoutAddingAnotherTask() {
        IngredientImage ready = new IngredientImage();
        ready.setCanonicalName("番茄");
        ready.setVerificationStatus(IngredientImageStatus.READY.name());
        when(imageMapper.selectMetadataByCanonicalName("番茄")).thenReturn(ready);

        service.ensureQueued(List.of("番茄"));

        verify(imageMapper, never()).insert(any(IngredientImage.class));
        verify(taskService, never()).enqueue(any(), any());
    }

    @Test
    void retriesRecentFailedImageWhenTheIngredientIsSearchedAgain() {
        IngredientImage failed = new IngredientImage();
        failed.setId(62L);
        failed.setCanonicalName("茄子");
        failed.setVerificationStatus(IngredientImageStatus.FAILED.name());
        failed.setFailureReason("候选图片不匹配");
        failed.setUpdatedAt(LocalDateTime.now());
        when(imageMapper.selectMetadataByCanonicalName("茄子")).thenReturn(failed);

        service.ensureQueuedAfterSearch(List.of("茄子"));

        verify(imageMapper).updateById(failed);
        assertThat(failed.getVerificationStatus()).isEqualTo("PENDING");
        assertThat(failed.getFailureReason()).isNull();
        verify(taskService).enqueue(62L, "茄子");
    }
}
