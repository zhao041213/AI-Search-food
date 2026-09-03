package com.example.food.admin.error;

import com.example.food.admin.error.dto.AdminErrorLogPageResponse;
import com.example.food.admin.error.dto.AdminErrorLogResponse;
import com.example.food.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/error-logs")
public class AdminErrorLogController {

    private final AdminErrorLogService service;

    public AdminErrorLogController(AdminErrorLogService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<AdminErrorLogPageResponse> list(
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return ApiResponse.ok(service.list(sourceType, keyword, from, to, limit, offset));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminErrorLogResponse> detail(@PathVariable Long id) {
        AdminErrorLogResponse response = service.find(id);
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "异常日志不存在");
        }
        return ApiResponse.ok(response);
    }
}
