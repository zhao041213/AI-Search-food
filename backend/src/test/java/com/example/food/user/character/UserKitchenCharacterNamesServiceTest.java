package com.example.food.user.character;

import com.example.food.user.character.dto.KitchenCharacterNamesRequest;
import com.example.food.user.character.dto.KitchenCharacterNamesResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserKitchenCharacterNamesServiceTest {

    @Mock
    private UserKitchenCharacterNamesMapper mapper;

    private UserKitchenCharacterNamesService service;

    @BeforeEach
    void setUp() {
        service = new UserKitchenCharacterNamesService(mapper, new ObjectMapper());
    }

    @Test
    void returnsDefaultNamesWhenUserHasNoCustomization() {
        when(mapper.selectById(7L)).thenReturn(null);

        KitchenCharacterNamesResponse result = service.get(7L);

        assertThat(result.hasCustomNames()).isFalse();
        assertThat(result.names()).containsEntry("chef", "阿灶");
        assertThat(result.names()).hasSize(11);
    }

    @Test
    void userDataIsIsolatedByUserId() {
        when(mapper.selectById(7L)).thenReturn(stored(7L, names("用户一")));
        when(mapper.selectById(8L)).thenReturn(null);

        assertThat(service.get(7L).names().get("chef")).isEqualTo("用户一");
        assertThat(service.get(8L).hasCustomNames()).isFalse();
        verify(mapper).selectById(7L);
        verify(mapper).selectById(8L);
    }

    @Test
    void trimsAndSavesAllValidNamesForCurrentUser() {
        when(mapper.selectById(7L)).thenReturn(null);

        KitchenCharacterNamesResponse result = service.save(7L, request("  新  主厨 "));

        ArgumentCaptor<UserKitchenCharacterNames> captor =
                ArgumentCaptor.forClass(UserKitchenCharacterNames.class);
        verify(mapper).insert(captor.capture());
        UserKitchenCharacterNames saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getNamesJson()).contains("新 主厨");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(result.names().get("chef")).isEqualTo("新 主厨");
        assertThat(result.hasCustomNames()).isTrue();
    }

    @Test
    void rejectsEmptyName() {
        Map<String, String> names = names("空值");
        names.put("chef", "  ");

        assertThatThrownBy(() -> service.save(7L, new KitchenCharacterNamesRequest(names)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("人物名称不能为空");
    }

    @Test
    void rejectsOverlongName() {
        Map<String, String> names = names("过长");
        names.put("chef", "超过六个字符啊");

        assertThatThrownBy(() -> service.save(7L, new KitchenCharacterNamesRequest(names)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("人物名称最多 6 个字符");
    }

    @Test
    void rejectsDuplicateName() {
        Map<String, String> names = names("重复");
        names.put("chef-helper", names.get("chef"));

        assertThatThrownBy(() -> service.save(7L, new KitchenCharacterNamesRequest(names)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("人物名称不能重复");
    }

    @Test
    void rejectsUnknownCharacterId() {
        Map<String, String> names = names("未知");
        names.put("unknown", "陌生人");
        names.remove("hot");

        assertThatThrownBy(() -> service.save(7L, new KitchenCharacterNamesRequest(names)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("包含未知人物编号");
    }

    @Test
    void rejectsMissingCharacterName() {
        Map<String, String> names = names("缺失");
        names.remove("hot");

        assertThatThrownBy(() -> service.save(7L, new KitchenCharacterNamesRequest(names)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("必须提供全部 11 个人物名称");
    }

    @Test
    void resetIsIdempotentAndReturnsDefaults() {
        KitchenCharacterNamesResponse first = service.reset(7L);
        KitchenCharacterNamesResponse second = service.reset(7L);

        verify(mapper, times(2)).deleteById(7L);
        assertThat(first).isEqualTo(second);
        assertThat(first.hasCustomNames()).isFalse();
        assertThat(first.names().get("chef")).isEqualTo("阿灶");
    }

    @Test
    void savingDefaultsClearsStoredCustomization() {
        KitchenCharacterNamesResponse result = service.save(
                7L,
                new KitchenCharacterNamesRequest(names("阿灶"))
        );

        verify(mapper).deleteById(7L);
        assertThat(result.hasCustomNames()).isFalse();
    }

    @Test
    void concurrentFirstSaveFallsBackToUpdate() {
        when(mapper.selectById(7L)).thenReturn(null);
        when(mapper.insert(any(UserKitchenCharacterNames.class)))
                .thenThrow(new DuplicateKeyException("duplicate user_id"));

        service.save(7L, request("并发"));

        verify(mapper).updateById(org.mockito.ArgumentMatchers.<UserKitchenCharacterNames>argThat(stored ->
                stored.getUserId().equals(7L) && stored.getCreatedAt() == null
        ));
    }

    @Test
    void readsStoredNamesAndReportsCustomization() {
        UserKitchenCharacterNames stored = stored(7L, names("已存"));
        when(mapper.selectById(7L)).thenReturn(stored);

        KitchenCharacterNamesResponse result = service.get(7L);

        assertThat(result.hasCustomNames()).isTrue();
        assertThat(result.names().get("chef")).isEqualTo("已存");
    }

    private KitchenCharacterNamesRequest request(String chefName) {
        return new KitchenCharacterNamesRequest(names(chefName));
    }

    private Map<String, String> names(String chefName) {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("chef", chefName);
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
        return names;
    }

    private UserKitchenCharacterNames stored(Long userId, Map<String, String> names) {
        UserKitchenCharacterNames stored = new UserKitchenCharacterNames();
        stored.setUserId(userId);
        stored.setNamesJson(write(names));
        stored.setCreatedAt(LocalDateTime.of(2026, 9, 1, 12, 0));
        stored.setUpdatedAt(LocalDateTime.of(2026, 9, 2, 12, 0));
        return stored;
    }

    private String write(Map<String, String> names) {
        try {
            return new ObjectMapper().writeValueAsString(names);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
