package com.example.food.notification;

import com.example.food.common.GlobalExceptionHandler;
import com.example.food.notification.dto.NotificationPageResponse;
import com.example.food.notification.dto.NotificationPreferenceResponse;
import com.example.food.notification.dto.NotificationResponse;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
import com.example.food.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private NotificationService service;

    @Test
    void listsAuthenticatedUsersOwnNotificationPage() throws Exception {
        authenticateUser();
        NotificationResponse item = new NotificationResponse(
                12L,
                NotificationType.PANTRY_EXPIRING.name(),
                "食材即将到期：番茄",
                "将在 2026-09-06 到期",
                "详情",
                "/pantry",
                NotificationStatus.UNREAD.name(),
                LocalDateTime.of(2026, 9, 4, 14, 0),
                null,
                null
        );
        when(service.list(7L, "unread", 2, 10))
                .thenReturn(new NotificationPageResponse(List.of(item), 11, 2, 10, 2));

        mockMvc.perform(get("/api/users/me/notifications")
                        .header("Authorization", "Bearer user-token")
                        .param("status", "unread")
                        .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(11))
                .andExpect(jsonPath("$.data.items[0].status").value("UNREAD"));

        verify(service).list(7L, "unread", 2, 10);
    }

    @Test
    void updatesAuthenticatedUsersNotificationPreferences() throws Exception {
        authenticateUser();
        when(service.updatePreferences(eq(7L), eq(new com.example.food.notification.dto.NotificationPreferenceRequest(false, true, false))))
                .thenReturn(new NotificationPreferenceResponse(false, true, false));

        mockMvc.perform(put("/api/users/me/notification-preferences")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.example.food.notification.dto.NotificationPreferenceRequest(false, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pantryExpiringEnabled").value(false))
                .andExpect(jsonPath("$.data.weeklyMenuPreparationEnabled").value(false));
    }

    @Test
    void requiresAllPreferenceFields() throws Exception {
        authenticateUser();

        mockMvc.perform(put("/api/users/me/notification-preferences")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pantryExpiringEnabled\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    private void authenticateUser() {
        when(jwtService.parseToken("user-token"))
                .thenReturn(new AuthPrincipal(7L, "13800138000", AppRole.USER));
    }
}
