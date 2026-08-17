package com.example.food.shopping;

import com.example.food.common.GlobalExceptionHandler;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
import com.example.food.security.SecurityConfig;
import com.example.food.shopping.dto.ShoppingItemCheckResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShoppingItemCheckController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ShoppingItemCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private ShoppingItemCheckService service;

    @Test
    void userCanReadOwnShoppingChecks() throws Exception {
        authenticateUser();
        when(service.list(7L, 88L)).thenReturn(List.of(new ShoppingItemCheckResponse("鸡蛋", true)));

        mockMvc.perform(get("/api/users/me/shopping-checks")
                        .header("Authorization", "Bearer user-token")
                        .param("searchLogId", "88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ingredientName").value("鸡蛋"))
                .andExpect(jsonPath("$.data[0].checked").value(true));
    }

    @Test
    void userCanSaveOwnShoppingCheck() throws Exception {
        authenticateUser();
        when(service.save(org.mockito.ArgumentMatchers.eq(7L), any()))
                .thenReturn(new ShoppingItemCheckResponse("番茄", true));

        mockMvc.perform(put("/api/users/me/shopping-checks")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "searchLogId": 88,
                                  "ingredientName": "西红柿",
                                  "checked": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ingredientName").value("番茄"))
                .andExpect(jsonPath("$.data.checked").value(true));

        verify(service).save(org.mockito.ArgumentMatchers.eq(7L), any());
    }

    @Test
    void userCanSaveAProcurementStatus() throws Exception {
        authenticateUser();
        when(service.save(org.mockito.ArgumentMatchers.eq(7L), any()))
                .thenReturn(new ShoppingItemCheckResponse("egg", "PURCHASING", false));

        mockMvc.perform(put("/api/users/me/shopping-checks")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "searchLogId": 88,
                                  "ingredientName": "egg",
                                  "status": "PURCHASING"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PURCHASING"))
                .andExpect(jsonPath("$.data.checked").value(false));

        verify(service).save(org.mockito.ArgumentMatchers.eq(7L), any());
    }

    @Test
    void missingSearchLogIdReturnsParameterError() throws Exception {
        authenticateUser();

        mockMvc.perform(get("/api/users/me/shopping-checks")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));
    }

    private void authenticateUser() {
        when(jwtService.parseToken("user-token"))
                .thenReturn(new AuthPrincipal(7L, "13800138000", AppRole.USER));
    }
}
