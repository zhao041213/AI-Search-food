package com.example.food.stats.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class IngredientImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngredientImageService.class);
    private static final Duration PENDING_RETRY_AFTER = Duration.ofMinutes(15);
    private static final Duration FAILED_RETRY_AFTER = Duration.ofHours(24);

    private final IngredientImageMapper imageMapper;
    private final IngredientImageTaskService taskService;
    private final boolean enabled;

    public IngredientImageService(
            IngredientImageMapper imageMapper,
            IngredientImageTaskService taskService,
            @Value("${app.ingredient-images.enabled:true}") boolean enabled
    ) {
        this.imageMapper = imageMapper;
        this.taskService = taskService;
        this.enabled = enabled;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureQueued(Collection<String> canonicalNames) {
        ensureQueued(canonicalNames, false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureQueuedAfterSearch(Collection<String> canonicalNames) {
        ensureQueued(canonicalNames, true);
    }

    private void ensureQueued(Collection<String> canonicalNames, boolean retryFailedImmediately) {
        if (!enabled || canonicalNames == null || canonicalNames.isEmpty()) {
            return;
        }
        Set<String> distinctNames = new LinkedHashSet<>();
        for (String name : canonicalNames) {
            if (StringUtils.hasText(name)) {
                distinctNames.add(name.trim());
            }
        }
        for (String canonicalName : distinctNames) {
            try {
                ensureOne(canonicalName, retryFailedImmediately);
            } catch (DuplicateKeyException exception) {
                LOGGER.debug("食材图片缓存已被其他请求创建，ingredient={}", canonicalName);
            } catch (RuntimeException exception) {
                LOGGER.warn("创建食材图片缓存任务失败，ingredient={}", canonicalName);
            }
        }
    }

    public IngredientImage findMetadata(String canonicalName) {
        if (!StringUtils.hasText(canonicalName)) {
            return null;
        }
        return imageMapper.selectMetadataByCanonicalName(canonicalName.trim());
    }

    public IngredientImage findReady(String canonicalName) {
        if (!StringUtils.hasText(canonicalName)) {
            return null;
        }
        IngredientImage image = imageMapper.selectByCanonicalName(canonicalName.trim());
        if (image == null
                || !IngredientImageStatus.READY.name().equals(image.getVerificationStatus())
                || image.getImageData() == null
                || image.getImageData().length == 0) {
            return null;
        }
        return image;
    }

    private void ensureOne(String canonicalName, boolean retryFailedImmediately) {
        LocalDateTime now = LocalDateTime.now();
        IngredientImage image = imageMapper.selectMetadataByCanonicalName(canonicalName);
        if (image == null) {
            image = new IngredientImage();
            image.setCanonicalName(canonicalName);
            image.setVerificationStatus(IngredientImageStatus.PENDING.name());
            image.setCreatedAt(now);
            image.setUpdatedAt(now);
            imageMapper.insert(image);
            enqueueAfterCommit(image.getId(), canonicalName);
            return;
        }
        if (!shouldRetry(image, now, retryFailedImmediately)) {
            return;
        }
        image.setVerificationStatus(IngredientImageStatus.PENDING.name());
        image.setFailureReason(null);
        image.setUpdatedAt(now);
        imageMapper.updateById(image);
        enqueueAfterCommit(image.getId(), canonicalName);
    }

    private boolean shouldRetry(
            IngredientImage image,
            LocalDateTime now,
            boolean retryFailedImmediately
    ) {
        String status = image.getVerificationStatus();
        if (IngredientImageStatus.READY.name().equals(status)) {
            return false;
        }
        LocalDateTime updatedAt = image.getUpdatedAt();
        if (updatedAt == null) {
            return true;
        }
        if (IngredientImageStatus.FAILED.name().equals(status)) {
            return retryFailedImmediately || updatedAt.plus(FAILED_RETRY_AFTER).isBefore(now);
        }
        return updatedAt.plus(PENDING_RETRY_AFTER).isBefore(now);
    }

    private void enqueueAfterCommit(Long imageId, String canonicalName) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            taskService.enqueue(imageId, canonicalName);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                taskService.enqueue(imageId, canonicalName);
            }
        });
    }
}
