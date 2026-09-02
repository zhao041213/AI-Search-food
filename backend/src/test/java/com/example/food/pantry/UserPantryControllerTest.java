package com.example.food.pantry;

import com.example.food.common.GlobalExceptionHandler;
import com.example.food.pantry.dto.PantryItemResponse;
import com.example.food.pantry.dto.PantryExpirySummaryResponse;
import com.example.food.pantry.dto.PantryReadinessResponse;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
import com.example.food.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserPantryController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserPantryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserPantryService service;

    @Test
    void userCanListOwnPantry() throws Exception {
        authenticateUser();
        when(service.list(7L)).thenReturn(List.of(new PantryItemResponse(
                3L, "番茄", "蔬菜", new BigDecimal("2"), "个", LocalDate.of(2026, 8, 31), null
        )));

        mockMvc.perform(get("/api/users/me/pantry")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ingredientName").value("番茄"))
                .andExpect(jsonPath("$.data[0].quantity").value(2));
    }

    @Test
    void userCanReadPantryExpiryAlerts() throws Exception {
        authenticateUser();
        when(service.expirySummary(7L)).thenReturn(new PantryExpirySummaryResponse(
                LocalDate.of(2026, 8, 31),
                7,
                List.of(new PantryItemResponse(
                        3L, "西兰花", "蔬菜", new BigDecimal("1"), "棵", LocalDate.of(2026, 8, 30), null
                )),
                List.of(new PantryItemResponse(
                        4L, "牛奶", "乳品", new BigDecimal("2"), "盒", LocalDate.of(2026, 9, 2), null
                ))
        ));

        mockMvc.perform(get("/api/users/me/pantry/expiry-alerts")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warningDays").value(7))
                .andExpect(jsonPath("$.data.expiredItems[0].ingredientName").value("西兰花"))
                .andExpect(jsonPath("$.data.expiringSoonItems[0].ingredientName").value("牛奶"));

        verify(service).expirySummary(7L);
    }

    @Test
    void userCanReadRecipePantryReadiness() throws Exception {
        authenticateUser();
        when(service.readiness(org.mockito.ArgumentMatchers.eq(7L), any())).thenReturn(new PantryReadinessResponse(
                2,
                1,
                1,
                1,
                50,
                List.of(new PantryReadinessResponse.Item(
                        "tomato", "2 piece", new BigDecimal("2"), "piece", new BigDecimal("2"), "piece",
                        "ENOUGH", "enough", false, true, List.of()
                ))
        ));

        mockMvc.perform(post("/api/users/me/pantry/readiness")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ingredients": [
                                    {"name": "tomato", "amount": "2 piece"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount").value(2))
                .andExpect(jsonPath("$.data.readinessPercent").value(50))
                .andExpect(jsonPath("$.data.items[0].status").value("ENOUGH"));

        verify(service).readiness(org.mockito.ArgumentMatchers.eq(7L), any());
    }

    @Test
    void emptyReadinessIngredientsReturnsParameterError() throws Exception {
        authenticateUser();

        mockMvc.perform(post("/api/users/me/pantry/readiness")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ingredients": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));
    }

    @Test
    void userCanCreateOwnPantryItem() throws Exception {
        authenticateUser();
        when(service.create(org.mockito.ArgumentMatchers.eq(7L), any())).thenReturn(new PantryItemResponse(
                3L, "番茄", "蔬菜", new BigDecimal("2"), "个", LocalDate.of(2026, 8, 31), null
        ));

        mockMvc.perform(post("/api/users/me/pantry")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ingredientName": "番茄",
                                  "category": "蔬菜",
                                  "quantity": 2,
                                  "unit": "个",
                                  "expireDate": "2026-08-31"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.ingredientName").value("番茄"));

        verify(service).create(org.mockito.ArgumentMatchers.eq(7L), any());
    }

    @Test
    void invalidPantryRequestReturnsParameterError() throws Exception {
        authenticateUser();

        mockMvc.perform(post("/api/users/me/pantry")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ingredientName": "", "quantity": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));
    }

    @Test
    void userCanDeleteOwnPantryItem() throws Exception {
        authenticateUser();

        mockMvc.perform(delete("/api/users/me/pantry/{id}", 3L)
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk());

        verify(service).delete(7L, 3L);
    }

    @Test
    void userCanConsumeOwnPantryItem() throws Exception {
        authenticateUser();
        when(service.consume(7L, 3L, new BigDecimal("0.5"))).thenReturn(new PantryItemResponse(
                3L, "番茄", "蔬菜", new BigDecimal("1.5"), "个", LocalDate.of(2026, 8, 31), null
        ));

        mockMvc.perform(post("/api/users/me/pantry/{id}/consume", 3L)
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 0.5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.quantity").value(1.5));

        verify(service).consume(7L, 3L, new BigDecimal("0.5"));
    }

    @Test
    void invalidConsumptionQuantityReturnsParameterError() throws Exception {
        authenticateUser();

        mockMvc.perform(post("/api/users/me/pantry/{id}/consume", 3L)
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));
    }

    private void authenticateUser() {
        when(jwtService.parseToken("user-token"))
                .thenReturn(new AuthPrincipal(7L, "13800138000", AppRole.USER));
    }
}
