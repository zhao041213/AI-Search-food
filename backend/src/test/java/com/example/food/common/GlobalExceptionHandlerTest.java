package com.example.food.common;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validationExceptionsReturnInvalidRequestEnvelope() throws Exception {
        mockMvc.perform(post("/handler-test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));
    }

    @Test
    void illegalArgumentReturnsMessageEnvelope() throws Exception {
        mockMvc.perform(get("/handler-test/illegal"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("bad input"));
    }

    @Test
    void malformedJsonReturnsInvalidRequestEnvelope() throws Exception {
        mockMvc.perform(post("/handler-test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));
    }

    @Test
    void missingBodyReturnsInvalidRequestEnvelope() throws Exception {
        mockMvc.perform(post("/handler-test/body")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));
    }

    @Test
    void typeMismatchReturnsInvalidRequestEnvelope() throws Exception {
        mockMvc.perform(get("/handler-test/number")
                        .param("value", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));
    }

    @Test
    void missingRequestParamReturnsInvalidRequestEnvelope() throws Exception {
        mockMvc.perform(get("/handler-test/required-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));
    }

    @Test
    void responseStatusExceptionPreservesStatusAndReason() throws Exception {
        mockMvc.perform(get("/handler-test/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("missing thing"));
    }

    @Test
    void missingResourcePreservesNotFoundStatus() throws Exception {
        mockMvc.perform(get("/handler-test/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Not Found"));
    }

    @Test
    void constraintViolationReturnsInvalidRequestEnvelope() throws Exception {
        mockMvc.perform(get("/handler-test/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));
    }

    @Test
    void unexpectedExceptionsReturnInternalServerErrorEnvelope() throws Exception {
        mockMvc.perform(get("/handler-test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    record TestRequest(@NotBlank String name) {
    }
}

@RestController
@RequestMapping("/handler-test")
class GlobalExceptionHandlerTestController {

    @PostMapping("/validate")
    ApiResponse<Void> validate(@Valid @RequestBody GlobalExceptionHandlerTest.TestRequest request) {
        return ApiResponse.ok(null);
    }

    @PostMapping("/body")
    ApiResponse<Void> body(@RequestBody GlobalExceptionHandlerTest.TestRequest request) {
        return ApiResponse.ok(null);
    }

    @GetMapping("/illegal")
    ApiResponse<Void> illegal() {
        throw new IllegalArgumentException("bad input");
    }

    @GetMapping("/number")
    ApiResponse<Void> number(@RequestParam int value) {
        return ApiResponse.ok(null);
    }

    @GetMapping("/required-param")
    ApiResponse<Void> requiredParam(@RequestParam String value) {
        return ApiResponse.ok(null);
    }

    @GetMapping("/status")
    ApiResponse<Void> status() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "missing thing");
    }

    @GetMapping("/constraint")
    ApiResponse<Void> constraint() {
        throw new ConstraintViolationException("invalid", Set.of());
    }

    @GetMapping("/unexpected")
    ApiResponse<Void> unexpected() {
        throw new RuntimeException("boom");
    }
}
