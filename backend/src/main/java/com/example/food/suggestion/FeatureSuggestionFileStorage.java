package com.example.food.suggestion;

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
public class FeatureSuggestionFileStorage {
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final Path directory;

    public FeatureSuggestionFileStorage(@Value("${app.feature-suggestions.storage-dir:./data/feature-suggestions}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public UploadedImage readAndValidate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "截图不能为空");
        if (file.getSize() > MAX_FILE_SIZE) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "截图大小不能超过 5MB");
        String type = normalizeType(file.getContentType());
        if (!TYPES.contains(type)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "截图仅支持 JPG、PNG、WebP 格式");
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "截图内容为空");
            return new UploadedImage(bytes, type, cleanName(file.getOriginalFilename()));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "截图读取失败，请重新选择", exception);
        }
    }

    public StoredFile store(UploadedImage image) {
        String storedName = UUID.randomUUID() + extension(image.contentType());
        Path target = directory.resolve(storedName).normalize();
        if (!directory.equals(target.getParent())) throw new IllegalArgumentException("截图路径不合法");
        try {
            Files.createDirectories(directory);
            Files.write(target, image.bytes(), StandardOpenOption.CREATE_NEW);
            return new StoredFile(image.originalName(), storedName, image.contentType(), (long) image.bytes().length, target.toString());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "截图保存失败", exception);
        }
    }

    public byte[] load(String storedName) {
        try { return Files.readAllBytes(resolve(storedName)); }
        catch (IOException exception) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "截图文件不存在", exception); }
    }

    public void deleteQuietly(String storedName) { if (!StringUtils.hasText(storedName)) return; try { Files.deleteIfExists(resolve(storedName)); } catch (IOException | IllegalArgumentException ignored) { } }
    private Path resolve(String name) { if (!StringUtils.hasText(name)) throw new IllegalArgumentException("截图文件名不能为空"); Path target = directory.resolve(name).normalize(); if (!directory.equals(target.getParent())) throw new IllegalArgumentException("截图路径不合法"); return target; }
    private String normalizeType(String value) { if (!StringUtils.hasText(value)) return ""; int separator = value.indexOf(';'); return (separator >= 0 ? value.substring(0, separator) : value).trim().toLowerCase(Locale.ROOT); }
    private String cleanName(String value) { String cleaned = StringUtils.cleanPath(value == null ? "" : value).replace('\\', '/'); int separator = cleaned.lastIndexOf('/'); String name = separator >= 0 ? cleaned.substring(separator + 1) : cleaned; return StringUtils.hasText(name) ? name.substring(0, Math.min(255, name.length())) : "suggestion-image"; }
    private String extension(String type) { return switch (type) { case "image/jpeg" -> ".jpg"; case "image/png" -> ".png"; case "image/webp" -> ".webp"; default -> throw new IllegalArgumentException("截图格式不支持"); }; }
    public record UploadedImage(byte[] bytes, String contentType, String originalName) { }
    public record StoredFile(String originalName, String storedName, String contentType, Long fileSize, String storagePath) { }
    public record StoredFileInfo(byte[] bytes, String contentType, String originalName) { }
}
