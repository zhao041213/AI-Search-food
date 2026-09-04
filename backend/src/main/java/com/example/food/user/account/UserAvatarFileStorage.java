package com.example.food.user.account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Component
public class UserAvatarFileStorage {
    public static final String PURPOSE = "USER_AVATAR";
    private static final long MAX_FILE_SIZE = 2L * 1024L * 1024L;
    private final Path root;

    public UserAvatarFileStorage(
            @Value("${app.user-avatars.storage-dir:./data/user-avatars}") String storageDirectory
    ) {
        this.root = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public StoredAvatar store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择头像图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("头像图片不能超过 2MB");
        }
        try {
            byte[] bytes = file.getBytes();
            String contentType = detectContentType(bytes);
            if (contentType == null) {
                throw new IllegalArgumentException("头像仅支持 JPG、PNG 或 WebP 图片");
            }
            String storedName = UUID.randomUUID().toString().replace("-", "") + extension(contentType);
            Path target = resolve(storedName);
            Files.createDirectories(root);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredAvatar(storedName, contentType, bytes.length, safeOriginalName(file.getOriginalFilename()), target);
        } catch (IOException exception) {
            throw new IllegalStateException("头像文件保存失败", exception);
        }
    }

    public byte[] load(String storedName) {
        try {
            return Files.readAllBytes(resolve(storedName));
        } catch (IOException exception) {
            throw new IllegalArgumentException("头像文件不存在");
        }
    }

    public void deleteQuietly(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(storedName));
        } catch (IOException | IllegalArgumentException ignored) {
            // Orphaned files are safe to clean up later because no user row references them.
        }
    }

    private Path resolve(String storedName) {
        if (storedName == null || storedName.isBlank() || storedName.contains("/") || storedName.contains("\\")) {
            throw new IllegalArgumentException("头像文件路径无效");
        }
        Path resolved = root.resolve(storedName).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("头像文件路径无效");
        }
        return resolved;
    }

    private String detectContentType(byte[] bytes) {
        if (startsWith(bytes, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            return "image/jpeg";
        }
        if (startsWith(bytes, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
            return "image/png";
        }
        if (bytes.length >= 12
                && ascii(bytes, 0, 4).equals("RIFF")
                && ascii(bytes, 8, 4).equals("WEBP")) {
            return "image/webp";
        }
        return null;
    }

    private boolean startsWith(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (bytes[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, java.nio.charset.StandardCharsets.US_ASCII).toUpperCase(Locale.ROOT);
    }

    private String extension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException("头像格式不受支持");
        };
    }

    private String safeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "avatar";
        }
        String normalized = Path.of(originalName).getFileName().toString();
        return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
    }

    public record StoredAvatar(
            String storedName,
            String contentType,
            long fileSize,
            String originalName,
            Path path
    ) {
    }
}
