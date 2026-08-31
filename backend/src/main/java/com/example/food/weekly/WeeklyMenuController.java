package com.example.food.weekly;

import com.example.food.common.ApiResponse;
import com.example.food.security.AuthPrincipal;
import com.example.food.weekly.dto.WeeklyMenuResponse;
import com.example.food.weekly.dto.WeeklyMenuAutoGenerateRequest;
import com.example.food.weekly.dto.WeeklyMenuSaveRequest;
import com.example.food.weekly.dto.WeeklyMenuShoppingStatusRequest;
import com.example.food.weekly.dto.WeeklyShoppingStatusResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/users/me/weekly-menu")
public class WeeklyMenuController {

    private final WeeklyMenuService service;

    public WeeklyMenuController(WeeklyMenuService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<WeeklyMenuResponse> get(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStart
    ) {
        return ApiResponse.ok(service.get(principal.id(), weekStart));
    }

    @PutMapping
    public ApiResponse<WeeklyMenuResponse> save(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody WeeklyMenuSaveRequest request
    ) {
        return ApiResponse.ok(service.save(principal.id(), request));
    }

    @PostMapping("/auto-generate")
    public ApiResponse<WeeklyMenuResponse> autoGenerate(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody WeeklyMenuAutoGenerateRequest request
    ) {
        return ApiResponse.ok(service.autoGenerate(principal.id(), request));
    }

    @PutMapping("/shopping-status")
    public ApiResponse<WeeklyShoppingStatusResponse> saveShoppingStatus(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody WeeklyMenuShoppingStatusRequest request
    ) {
        return ApiResponse.ok(service.saveShoppingStatus(principal.id(), request));
    }

    @DeleteMapping
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStart
    ) {
        service.delete(principal.id(), weekStart);
        return ApiResponse.ok(null);
    }
}
