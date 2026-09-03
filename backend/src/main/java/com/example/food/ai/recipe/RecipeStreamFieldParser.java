package com.example.food.ai.recipe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads completed top-level JSON fields from an incrementally received recipe.
 * A field is returned only once, after its complete JSON value is available.
 */
public final class RecipeStreamFieldParser {

    private final ObjectMapper objectMapper;
    private final StringBuilder content = new StringBuilder();
    private final Map<String, JsonNode> emittedFields = new LinkedHashMap<>();

    public RecipeStreamFieldParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, JsonNode> accept(String delta) {
        if (delta == null || delta.isEmpty()) {
            return Map.of();
        }
        content.append(delta);
        Map<String, JsonNode> newlyCompleted = new LinkedHashMap<>();
        scan(newlyCompleted);
        return Map.copyOf(newlyCompleted);
    }

    public String content() {
        return content.toString();
    }

    private void scan(Map<String, JsonNode> newlyCompleted) {
        int cursor = skipWhitespace(content, 0);
        if (cursor >= content.length() || content.charAt(cursor) != '{') {
            return;
        }
        cursor++;
        while (true) {
            cursor = skipWhitespace(content, cursor);
            if (cursor >= content.length() || content.charAt(cursor) == '}') {
                return;
            }
            if (content.charAt(cursor) != '"') {
                return;
            }
            int keyEnd = findStringEnd(content, cursor);
            if (keyEnd < 0) {
                return;
            }
            String key;
            try {
                key = objectMapper.readValue(content.substring(cursor, keyEnd + 1), String.class);
            } catch (IOException exception) {
                return;
            }
            cursor = skipWhitespace(content, keyEnd + 1);
            if (cursor >= content.length() || content.charAt(cursor) != ':') {
                return;
            }
            int valueStart = skipWhitespace(content, cursor + 1);
            int valueEnd = findValueEnd(content, valueStart);
            if (valueEnd < 0) {
                return;
            }
            if (!emittedFields.containsKey(key)) {
                try {
                    JsonNode value = objectMapper.readTree(content.substring(valueStart, valueEnd));
                    if (value != null) {
                        emittedFields.put(key, value);
                        newlyCompleted.put(key, value);
                    }
                } catch (IOException | RuntimeException exception) {
                    return;
                }
            }
            cursor = skipWhitespace(content, valueEnd);
            if (cursor >= content.length()) {
                return;
            }
            char separator = content.charAt(cursor);
            if (separator == ',') {
                cursor++;
                continue;
            }
            if (separator == '}') {
                return;
            }
            return;
        }
    }

    private int findValueEnd(CharSequence source, int start) {
        if (start >= source.length()) {
            return -1;
        }
        char first = source.charAt(start);
        if (first == '"') {
            int stringEnd = findStringEnd(source, start);
            return stringEnd < 0 ? -1 : stringEnd + 1;
        }
        if (first == '{' || first == '[') {
            char opening = first;
            char closing = first == '{' ? '}' : ']';
            int depth = 0;
            boolean escaped = false;
            boolean inString = false;
            for (int index = start; index < source.length(); index++) {
                char current = source.charAt(index);
                if (inString) {
                    if (escaped) {
                        escaped = false;
                    } else if (current == '\\') {
                        escaped = true;
                    } else if (current == '"') {
                        inString = false;
                    }
                    continue;
                }
                if (current == '"') {
                    inString = true;
                } else if (current == opening) {
                    depth++;
                } else if (current == closing) {
                    depth--;
                    if (depth == 0) {
                        return index + 1;
                    }
                }
            }
            return -1;
        }
        for (int index = start; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == ',' || current == '}') {
                return index;
            }
        }
        return source.length();
    }

    private int findStringEnd(CharSequence source, int start) {
        boolean escaped = false;
        for (int index = start + 1; index < source.length(); index++) {
            char current = source.charAt(index);
            if (escaped) {
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == '"') {
                return index;
            }
        }
        return -1;
    }

    private int skipWhitespace(CharSequence source, int start) {
        int cursor = start;
        while (cursor < source.length() && Character.isWhitespace(source.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }
}
