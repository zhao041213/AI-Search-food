package com.example.food.ai.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Synchronizes the editable text-recipe settings into the project's .env file. */
@Component
public class DotEnvSynchronizer {

    private static final Pattern ENV_KEY = Pattern.compile("^[A-Z_][A-Z0-9_]*$");

    private final Path envFile;

    @Autowired
    public DotEnvSynchronizer(@Value("${app.ai.env-file:../.env}") String envFile) {
        this(Path.of(envFile));
    }

    public DotEnvSynchronizer(Path envFile) {
        this.envFile = envFile.toAbsolutePath().normalize();
    }

    public synchronized void synchronize(AiModelConfig config) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("AI_TEXT_RECIPE_PROVIDER", config.getProvider());
        values.put("AI_TEXT_RECIPE_PROTOCOL", config.getApiProtocol());
        values.put("AI_TEXT_RECIPE_MODEL", config.getModelName());
        values.put("AI_TEXT_RECIPE_ENDPOINT", config.getEndpointUrl());
        if (hasText(config.getApiKey())) {
            values.put("AI_TEXT_RECIPE_API_KEY", config.getApiKey().trim());
        }

        try {
            String original = Files.exists(envFile)
                    ? Files.readString(envFile, StandardCharsets.UTF_8)
                    : "";
            String updated = upsert(original, values);
            if (updated.equals(original)) {
                return;
            }
            Path parent = envFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            // Docker binds the project .env as a writable file, while its parent directory
            // can remain non-writable for the app user. Update the existing file in place
            // instead of creating a sibling temporary file in that directory.
            Files.writeString(envFile, updated, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new IllegalStateException("AI 配置已保存，但 .env 同步失败，请检查文件权限", exception);
        }
    }

    String upsert(String content, Map<String, String> values) {
        String lineEnding = content.contains("\r\n") ? "\r\n" : "\n";
        String[] lines = content.split("\\r?\\n", -1);
        StringBuilder result = new StringBuilder(content.length() + values.size() * 48);
        Map<String, Boolean> updated = new LinkedHashMap<>();
        values.keySet().forEach(key -> updated.put(key, false));

        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            String key = envKey(line);
            if (key != null && values.containsKey(key)) {
                result.append(key).append('=').append(encode(values.get(key)));
                updated.put(key, true);
            } else {
                result.append(line);
            }
            if (index < lines.length - 1) {
                result.append(lineEnding);
            }
        }

        boolean hasExistingContent = !content.isEmpty();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (Boolean.TRUE.equals(updated.get(entry.getKey()))) {
                continue;
            }
            if (hasExistingContent && !result.toString().endsWith(lineEnding)) {
                result.append(lineEnding);
            }
            result.append(entry.getKey()).append('=').append(encode(entry.getValue())).append(lineEnding);
            hasExistingContent = true;
        }
        return result.toString();
    }

    private String envKey(String line) {
        String candidate = line.trim();
        if (candidate.startsWith("export ")) {
            candidate = candidate.substring("export ".length()).trim();
        }
        int separator = candidate.indexOf('=');
        if (separator <= 0) {
            return null;
        }
        String key = candidate.substring(0, separator).trim();
        return ENV_KEY.matcher(key).matches() ? key : null;
    }

    private String encode(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.matches("[A-Za-z0-9_./:@+?=&%~-]+")) {
            return trimmed;
        }
        return "\"" + trimmed.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
