package com.example.food.review;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class FinishedDishReviewFileStorage {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Path storageDirectory;

    public FinishedDishReviewFileStorage(
            @Value("${app.finished-dish-reviews.storage-dir:./data/finished-dish-reviews}")
            String storageDirectory
    ) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public UploadedImage readAndValidate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传成品图");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "成品图大小不能超过 5MB");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (!SUPPORTED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 JPG、PNG、WebP 格式的成品图");
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "成品图内容为空");
            }
            return new UploadedImage(bytes, contentType, originalName(file.getOriginalFilename()));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法读取成品图，请重新选择图片", exception);
        }
    }

    public StoredFile store(UploadedImage image) {
        String storedName = UUID.randomUUID() + extensionFor(image.contentType());
        Path target = resolve(storedName);
        try {
            Files.createDirectories(storageDirectory);
            Files.write(target, image.bytes(), StandardOpenOption.CREATE_NEW);
            return new StoredFile(
                    image.originalName(),
                    storedName,
                    image.contentType(),
                    (long) image.bytes().length,
                    target.toString()
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "成品图保存失败", exception);
        }
    }

    public byte[] load(String storedName) {
        Path target = resolve(storedName);
        try {
            if (!Files.isRegularFile(target)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "成品图文件不存在");
            }
            return Files.readAllBytes(target);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法读取成品图文件", exception);
        }
    }

    public void deleteQuietly(String storedName) {
        if (!StringUtils.hasText(storedName)) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(storedName));
        } catch (IOException | IllegalArgumentException ignored) {
            // The database transaction remains the source of truth if cleanup cannot complete.
        }
    }

    private Path resolve(String storedName) {
        if (!StringUtils.hasText(storedName)) {
            throw new IllegalArgumentException("成品图文件名不能为空");
        }
        Path target = storageDirectory.resolve(storedName).normalize();
        if (!storageDirectory.equals(target.getParent())) {
            throw new IllegalArgumentException("成品图文件路径不合法");
        }
        return target;
    }

    private String originalName(String value) {
        String cleaned = StringUtils.cleanPath(value == null ? "" : value).replace('\\', '/');
        int separator = cleaned.lastIndexOf('/');
        String name = separator >= 0 ? cleaned.substring(separator + 1) : cleaned;
        return StringUtils.hasText(name) ? name.substring(0, Math.min(name.length(), 255)) : "finished-dish-image";
    }

    private String normalizeContentType(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        int separator = value.indexOf(';');
        return (separator >= 0 ? value.substring(0, separator) : value).trim().toLowerCase(Locale.ROOT);
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException("不支持的成品图格式");
        };
    }

    public record UploadedImage(byte[] bytes, String contentType, String originalName) {
    }

    public record StoredFile(
            String originalName,
            String storedName,
            String contentType,
            Long fileSize,
            String storagePath
    ) {
    }
}
