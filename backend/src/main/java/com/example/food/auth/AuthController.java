package com.example.food.auth;

import com.example.food.auth.dto.AdminLoginRequest;
import com.example.food.auth.dto.AuthResponse;
import com.example.food.auth.dto.PhoneCodeRequest;
import com.example.food.auth.dto.PhoneCodeResponse;
import com.example.food.auth.dto.PhoneLoginRequest;
import com.example.food.auth.dto.PhoneRegistrationRequest;
import com.example.food.auth.verification.SmsSendResult;
import com.example.food.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/user/register/code")
    public ApiResponse<PhoneCodeResponse> issueRegistrationCode(@Valid @RequestBody PhoneCodeRequest request) {
        return ApiResponse.ok(codeResponse(authService.issueRegistrationCode(request.phone())));
    }

    @PostMapping("/user/register")
    public ApiResponse<AuthResponse> registerUser(@Valid @RequestBody PhoneRegistrationRequest request) {
        return ApiResponse.ok(authService.registerUser(request));
    }

    @PostMapping("/user/code")
    public ApiResponse<PhoneCodeResponse> issueUserCode(@Valid @RequestBody PhoneCodeRequest request) {
        return ApiResponse.ok(codeResponse(authService.issueLoginCode(request.phone())));
    }

    @PostMapping("/user/login")
    public ApiResponse<AuthResponse> loginUser(@Valid @RequestBody PhoneLoginRequest request) {
        return ApiResponse.ok(authService.loginUser(request));
    }

    @PostMapping("/admin/login")
    public ApiResponse<AuthResponse> loginAdmin(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.ok(authService.loginAdmin(request));
    }

    private PhoneCodeResponse codeResponse(SmsSendResult result) {
        return new PhoneCodeResponse(result.code(), result.retryAfterSeconds());
    }
}
