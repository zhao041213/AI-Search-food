package com.example.food.user.character;

import com.example.food.common.GlobalExceptionHandler;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
import com.example.food.security.SecurityConfig;
import com.example.food.user.character.dto.KitchenCharacterNamesRequest;
import com.example.food.user.character.dto.KitchenCharacterNamesResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserKitchenCharacterNamesController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserKitchenCharacterNamesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserKitchenCharacterNamesService service;

    @Test
    void userCanReadOwnNames() throws Exception {
        authenticateUser();
        when(service.get(7L)).thenReturn(new KitchenCharacterNamesResponse(names("云端主厨"), true));

        mockMvc.perform(get("/api/users/me/kitchen-character-names")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.names.chef").value("云端主厨"))
                .andExpect(jsonPath("$.data.hasCustomNames").value(true));

        verify(service).get(7L);
    }

    @Test
    void userCanOverwriteOwnNamesWithoutSupplyingUserId() throws Exception {
        authenticateUser();
        KitchenCharacterNamesRequest request = new KitchenCharacterNamesRequest(names("新主厨"));
        when(service.save(eq(7L), any(KitchenCharacterNamesRequest.class)))
                .thenReturn(new KitchenCharacterNamesResponse(request.names(), true));

        mockMvc.perform(put("/api/users/me/kitchen-character-names")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.names.chef").value("新主厨"));

        verify(service).save(eq(7L), eq(request));
    }

    @Test
    void userCanResetOwnNames() throws Exception {
        authenticateUser();
        when(service.reset(7L)).thenReturn(new KitchenCharacterNamesResponse(names("阿灶"), false));

        mockMvc.perform(delete("/api/users/me/kitchen-character-names")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasCustomNames").value(false))
                .andExpect(jsonPath("$.data.names.chef").value("阿灶"));

        verify(service).reset(7L);
    }

    @Test
    void unauthenticatedUserCannotReadNames() throws Exception {
        mockMvc.perform(get("/api/users/me/kitchen-character-names"))
                .andExpect(status().isUnauthorized());
    }

    private void authenticateUser() {
        when(jwtService.parseToken("user-token"))
                .thenReturn(new AuthPrincipal(7L, "13800138000", AppRole.USER));
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
}
