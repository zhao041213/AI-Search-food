package com.example.food.ai.ingredient;

import com.example.food.ai.ingredient.dto.IngredientRecognitionResponse;
import com.example.food.ai.qwen.QwenVisionClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Service
public class IngredientRecognitionService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final QwenVisionClient qwenVisionClient;

    public IngredientRecognitionService(QwenVisionClient qwenVisionClient) {
        this.qwenVisionClient = qwenVisionClient;
    }

    public IngredientRecognitionResponse recognize(MultipartFile file) {
        validate(file);
        try {
            return qwenVisionClient.recognizeIngredients(normalizedContentType(file), file.getBytes());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片读取失败", exception);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传食材图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片大小不能超过 5MB");
        }
        if (!SUPPORTED_IMAGE_TYPES.contains(normalizedContentType(file))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 JPG、PNG、WebP 图片");
        }
    }

    private String normalizedContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
    }
}
