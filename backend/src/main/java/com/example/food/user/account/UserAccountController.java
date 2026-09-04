package com.example.food.user.account;

import com.example.food.auth.dto.PhoneCodeResponse;
import com.example.food.auth.verification.SmsSendResult;
import com.example.food.common.ApiResponse;
import com.example.food.common.RequestIp;
import com.example.food.security.AuthPrincipal;
import com.example.food.user.account.dto.AccountCancellationRequest;
import com.example.food.user.account.dto.UserAccountResponse;
import com.example.food.user.account.dto.UserProfileUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me/account")
public class UserAccountController {
    private final UserAccountService service;

    public UserAccountController(UserAccountService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<UserAccountResponse> getAccount(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok(service.getAccount(principal.id()));
    }

    @PatchMapping("/profile")
    public ApiResponse<UserAccountResponse> updateProfile(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody UserProfileUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(service.updateProfile(principal.id(), request, RequestIp.resolve(httpRequest)));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserAccountResponse> uploadAvatar(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestPart("image") MultipartFile image,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(service.uploadAvatar(principal.id(), image, RequestIp.resolve(httpRequest)));
    }

    @GetMapping("/avatar")
    public ResponseEntity<byte[]> getAvatar(@AuthenticationPrincipal AuthPrincipal principal) {
        UserAccountService.AvatarImage image = service.getAvatar(principal.id());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.bytes());
    }

    @DeleteMapping("/avatar")
    public ApiResponse<UserAccountResponse> deleteAvatar(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.ok(service.deleteAvatar(principal.id(), RequestIp.resolve(httpRequest)));
    }

    @PostMapping("/logout-all")
    public ApiResponse<Void> logoutAllDevices(
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        service.logoutAllDevices(principal.id(), RequestIp.resolve(httpRequest));
        return ApiResponse.ok(null);
    }

    @PostMapping("/cancel/code")
    public ApiResponse<PhoneCodeResponse> issueCancellationCode(@AuthenticationPrincipal AuthPrincipal principal) {
        SmsSendResult result = service.issueCancellationCode(principal.id());
        return ApiResponse.ok(new PhoneCodeResponse(result.code(), result.retryAfterSeconds()));
    }

    @PostMapping("/cancel")
    public ApiResponse<Void> cancelAccount(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody AccountCancellationRequest request,
            HttpServletRequest httpRequest
    ) {
        service.cancelAccount(principal.id(), request, RequestIp.resolve(httpRequest));
        return ApiResponse.ok(null);
    }
}
