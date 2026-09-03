package com.example.food.stats.image;

import com.example.food.admin.error.AdminErrorLogService;
import com.example.food.ai.qwen.QwenVisionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class IngredientImageTaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngredientImageTaskService.class);
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final double MIN_VERIFICATION_CONFIDENCE = 0.78d;
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final IngredientImageMapper imageMapper;
    private final IngredientImageProvider imageProvider;
    private final QwenVisionClient qwenVisionClient;
    private final AdminErrorLogService errorLogService;

    @Autowired
    public IngredientImageTaskService(
            IngredientImageMapper imageMapper,
            IngredientImageProvider imageProvider,
            QwenVisionClient qwenVisionClient,
            AdminErrorLogService errorLogService
    ) {
        this.imageMapper = imageMapper;
        this.imageProvider = imageProvider;
        this.qwenVisionClient = qwenVisionClient;
        this.errorLogService = errorLogService;
    }

    public IngredientImageTaskService(
            IngredientImageMapper imageMapper,
            IngredientImageProvider imageProvider,
            QwenVisionClient qwenVisionClient
    ) {
        this(imageMapper, imageProvider, qwenVisionClient, null);
    }

    @Async("ingredientImageTaskExecutor")
    @Transactional
    public void enqueue(Long imageId, String canonicalName) {
        if (imageId == null || !StringUtils.hasText(canonicalName)) {
            return;
        }
        try {
            List<IngredientImageCandidate> candidates = imageProvider.findCandidates(canonicalName);
            if (candidates == null || candidates.isEmpty()) {
                throw new IllegalStateException("未找到可用候选图片");
            }
            boolean verificationAttempted = false;
            for (IngredientImageCandidate candidate : candidates) {
                IngredientImageContent content;
                try {
                    content = imageProvider.download(candidate);
                    validateContent(content);
                } catch (RuntimeException exception) {
                    LOGGER.debug("跳过不可用的食材候选图片，ingredient={}, imageUrl={}",
                            canonicalName, candidate.imageUrl(), exception);
                    continue;
                }
                verificationAttempted = true;
                QwenVisionClient.IngredientImageVerification verification =
                        qwenVisionClient.verifyIngredientImage(
                                content.contentType(),
                                content.bytes(),
                                canonicalName
                        );
                if (!verification.matches() || verification.confidence() < MIN_VERIFICATION_CONFIDENCE) {
                    continue;
                }
                storeReady(imageId, candidate, content, verification);
                return;
            }
            String failureReason = verificationAttempted
                    ? "所有候选图片均未通过千问视觉校验"
                    : "候选图片均无法下载或格式不受支持";
            markFailed(imageId, failureReason);
            if (errorLogService != null) {
                errorLogService.recordFailure(
                        verificationAttempted ? AdminErrorLogService.AI : AdminErrorLogService.TOOL,
                        verificationAttempted ? "QwenVisionClient" : "WikimediaIngredientImageProvider",
                        failureReason,
                        null,
                        null,
                        "ASYNC",
                        "/internal/tools/ingredient-image-cache",
                        502,
                        null
                );
            }
        } catch (Exception exception) {
            LOGGER.warn("食材图片缓存任务失败，ingredientImageId={}, ingredient={}", imageId, canonicalName);
            markFailed(imageId, shortReason(exception));
            if (errorLogService != null) {
                errorLogService.recordException(
                        exception,
                        null,
                        "ASYNC",
                        "/internal/tools/ingredient-image-cache",
                        500,
                        null
                );
            }
        }
    }

    private void storeReady(
            Long imageId,
            IngredientImageCandidate candidate,
            IngredientImageContent content,
            QwenVisionClient.IngredientImageVerification verification
    ) {
        IngredientImage image = imageMapper.selectById(imageId);
        if (image == null) {
            return;
        }
        image.setImageData(content.bytes());
        image.setContentType(content.contentType());
        image.setSourceProvider(candidate.provider());
        image.setSourceUrl(candidate.sourceUrl());
        image.setVerificationStatus(IngredientImageStatus.READY.name());
        image.setVerificationScore(BigDecimal.valueOf(verification.confidence()));
        image.setFailureReason(null);
        image.setUpdatedAt(LocalDateTime.now());
        imageMapper.updateById(image);
    }

    private void validateContent(IngredientImageContent content) {
        if (content == null || content.bytes() == null || content.bytes().length == 0) {
            throw new IllegalStateException("候选图片内容为空");
        }
        if (content.bytes().length > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalStateException("候选图片超过 5MB");
        }
        if (!SUPPORTED_CONTENT_TYPES.contains(content.contentType())) {
            throw new IllegalStateException("候选图片格式不受支持");
        }
    }

    @Transactional
    void markFailed(Long imageId, String reason) {
        IngredientImage image = imageMapper.selectById(imageId);
        if (image == null) {
            return;
        }
        image.setImageData(null);
        image.setVerificationStatus(IngredientImageStatus.FAILED.name());
        image.setFailureReason(reason);
        image.setUpdatedAt(LocalDateTime.now());
        imageMapper.updateById(image);
    }

    private String shortReason(Exception exception) {
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) {
            return "图片缓存任务执行失败";
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
    }
}
