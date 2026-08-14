package com.example.food.user.preference;

import com.example.food.common.GlobalExceptionHandler;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import com.example.food.security.JwtService;
import com.example.food.security.SecurityConfig;
import com.example.food.user.preference.dto.DietPreferenceRequest;
import com.example.food.user.preference.dto.DietPreferenceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserDietPreferenceController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserDietPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDietPreferenceService service;

    @Test
    void userCanGetOwnDefaultPreference() throws Exception {
        authenticateUser();
        when(service.get(7L)).thenReturn(DietPreferenceResponse.empty());

        mockMvc.perform(get("/api/users/me/diet-preferences")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.taste").value("any"))
                .andExpect(jsonPath("$.data.defaultGoal").value("balanced"))
                .andExpect(jsonPath("$.data.avoidIngredients").isEmpty())
                .andExpect(jsonPath("$.data.allergenIngredients").isEmpty());
    }

    @Test
    void userCanOverwriteOwnPreference() throws Exception {
        authenticateUser();
        DietPreferenceRequest request = new DietPreferenceRequest(
                "light",
                "fat_loss",
                List.of(" 香菜 "),
                List.of("花生")
        );
        DietPreferenceResponse response = new DietPreferenceResponse(
                "light",
                "fat_loss",
                List.of("香菜"),
                List.of("花生")
        );
        when(service.save(7L, request)).thenReturn(response);

        mockMvc.perform(put("/api/users/me/diet-preferences")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taste").value("light"))
                .andExpect(jsonPath("$.data.avoidIngredients[0]").value("香菜"));

        verify(service).save(7L, request);
    }

    @Test
    void invalidTasteReturnsParameterError() throws Exception {
        authenticateUser();

        mockMvc.perform(put("/api/users/me/diet-preferences")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taste": "salty",
                                  "defaultGoal": "balanced",
                                  "avoidIngredients": [],
                                  "allergenIngredients": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));
    }

    @Test
    void oversizedIngredientListReturnsParameterError() throws Exception {
        authenticateUser();
        List<String> ingredients = java.util.stream.IntStream.range(0, 21)
                .mapToObj(index -> "食材" + index)
                .toList();
        DietPreferenceRequest request = new DietPreferenceRequest(
                "any",
                "balanced",
                ingredients,
                List.of()
        );

        mockMvc.perform(put("/api/users/me/diet-preferences")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void oversizedIngredientNameReturnsParameterError() throws Exception {
        authenticateUser();
        DietPreferenceRequest request = new DietPreferenceRequest(
                "any",
                "balanced",
                List.of("食".repeat(33)),
                List.of()
        );

        mockMvc.perform(put("/api/users/me/diet-preferences")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void ingredientNameIsTrimmedBeforeLengthValidation() throws Exception {
        authenticateUser();
        String ingredient = "食".repeat(32);
        DietPreferenceRequest normalizedRequest = new DietPreferenceRequest(
                "any",
                "balanced",
                List.of(ingredient),
                List.of()
        );
        when(service.save(7L, normalizedRequest)).thenReturn(new DietPreferenceResponse(
                "any",
                "balanced",
                List.of(ingredient),
                List.of()
        ));

        mockMvc.perform(put("/api/users/me/diet-preferences")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taste": "any",
                                  "defaultGoal": "balanced",
                                  "avoidIngredients": [" %s "],
                                  "allergenIngredients": []
                                }
                                """.formatted(ingredient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avoidIngredients[0]").value(ingredient));

        verify(service).save(7L, normalizedRequest);
    }

    private void authenticateUser() {
        when(jwtService.parseToken("user-token"))
                .thenReturn(new AuthPrincipal(7L, "13800138000", AppRole.USER));
    }
}
