package com.example.food.agent;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Set;

public record AgentAttachment(String originalFilename, String contentType, byte[] bytes) {

    private static final long MAX_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> SUPPORTED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    public AgentAttachment {
        bytes = bytes == null ? new byte[0] : bytes.clone();
    }

    public static AgentAttachment from(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "附件仅支持 JPG、PNG、WebP 图片");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "附件图片不能超过 5MB");
        }
        try {
            return new AgentAttachment(file.getOriginalFilename(), contentType, file.getBytes());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "附件图片读取失败", exception);
        }
    }

    public MultipartFile asMultipartFile() {
        return new InMemoryMultipartFile(originalFilename, contentType, bytes);
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }

    private static final class InMemoryMultipartFile implements MultipartFile {
        private final String filename;
        private final String contentType;
        private final byte[] bytes;

        private InMemoryMultipartFile(String filename, String contentType, byte[] bytes) {
            this.filename = filename == null ? "agent-image" : filename;
            this.contentType = contentType;
            this.bytes = bytes.clone();
        }

        @Override public String getName() { return "image"; }
        @Override public String getOriginalFilename() { return filename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return bytes.length == 0; }
        @Override public long getSize() { return bytes.length; }
        @Override public byte[] getBytes() { return bytes.clone(); }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
        @Override public void transferTo(File destination) throws IOException { Files.write(destination.toPath(), bytes); }
    }
}
