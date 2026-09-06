package com.example.food.user.character;

import com.example.food.user.character.dto.KitchenCharacterNamesRequest;
import com.example.food.user.character.dto.KitchenCharacterNamesResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class UserKitchenCharacterNamesService {

    private static final int MAX_NAME_LENGTH = 6;
    private static final Map<String, String> DEFAULT_NAMES = defaultNames();

    private final UserKitchenCharacterNamesMapper mapper;
    private final ObjectMapper objectMapper;

    public UserKitchenCharacterNamesService(
            UserKitchenCharacterNamesMapper mapper,
            ObjectMapper objectMapper
    ) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public KitchenCharacterNamesResponse get(Long userId) {
        UserKitchenCharacterNames stored = mapper.selectById(userId);
        if (stored == null) {
            return defaultsResponse();
        }
        try {
            Map<String, String> names = normalizeAndValidate(readNames(stored.getNamesJson()));
            return response(names);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return defaultsResponse();
        }
    }

    @Transactional
    public KitchenCharacterNamesResponse save(Long userId, KitchenCharacterNamesRequest request) {
        Map<String, String> names = normalizeAndValidate(request == null ? null : request.names());
        if (names.equals(DEFAULT_NAMES)) {
            mapper.deleteById(userId);
            return defaultsResponse();
        }

        UserKitchenCharacterNames stored = mapper.selectById(userId);
        boolean exists = stored != null;
        LocalDateTime now = LocalDateTime.now();
        if (!exists) {
            stored = new UserKitchenCharacterNames();
            stored.setUserId(userId);
            stored.setCreatedAt(now);
        }
        stored.setNamesJson(writeNames(names));
        stored.setUpdatedAt(now);

        if (exists) {
            mapper.updateById(stored);
        } else {
            try {
                mapper.insert(stored);
            } catch (DuplicateKeyException exception) {
                stored.setCreatedAt(null);
                mapper.updateById(stored);
            }
        }
        return response(names);
    }

    @Transactional
    public KitchenCharacterNamesResponse reset(Long userId) {
        mapper.deleteById(userId);
        return defaultsResponse();
    }

    private KitchenCharacterNamesResponse response(Map<String, String> names) {
        return new KitchenCharacterNamesResponse(names, !names.equals(DEFAULT_NAMES));
    }

    private KitchenCharacterNamesResponse defaultsResponse() {
        return new KitchenCharacterNamesResponse(DEFAULT_NAMES, false);
    }

    private Map<String, String> normalizeAndValidate(Map<String, String> values) {
        if (values == null) {
            throw new IllegalArgumentException("人物名称不能为空");
        }
        Set<String> unknownIds = new HashSet<>(values.keySet());
        unknownIds.removeAll(DEFAULT_NAMES.keySet());
        if (!unknownIds.isEmpty()) {
            throw new IllegalArgumentException("包含未知人物编号");
        }
        if (values.size() != DEFAULT_NAMES.size()) {
            throw new IllegalArgumentException("必须提供全部 11 个人物名称");
        }

        Map<String, String> names = new LinkedHashMap<>();
        Map<String, String> ownersByName = new LinkedHashMap<>();
        for (String characterId : DEFAULT_NAMES.keySet()) {
            String name = normalize(values.get(characterId));
            if (name.isEmpty()) {
                throw new IllegalArgumentException("人物名称不能为空");
            }
            if (name.length() > MAX_NAME_LENGTH) {
                throw new IllegalArgumentException("人物名称最多 6 个字符");
            }
            String duplicateKey = name.toLowerCase(Locale.ROOT);
            if (ownersByName.containsKey(duplicateKey)) {
                throw new IllegalArgumentException("人物名称不能重复");
            }
            ownersByName.put(duplicateKey, characterId);
            names.put(characterId, name);
        }
        return names;
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip().replaceAll("\\s+", " ");
    }

    private Map<String, String> readNames(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("人物名称数据为空");
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("人物名称数据格式错误", exception);
        }
    }

    private String writeNames(Map<String, String> names) {
        try {
            return objectMapper.writeValueAsString(names);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("人物名称序列化失败", exception);
        }
    }

    private static Map<String, String> defaultNames() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("chef", "阿灶");
        names.put("chef-helper", "小灶");
        names.put("chef-recipes", "小谱");
        names.put("chef-nutrition", "小衡");
        names.put("pantry", "小仓");
        names.put("recipes", "阿笺");
        names.put("nutrition", "衡衡");
        names.put("weekly", "周周");
        names.put("review", "味味");
        names.put("account", "小管");
        names.put("hot", "椒椒");
        return Map.copyOf(names);
    }
}
