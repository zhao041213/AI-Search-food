package com.example.food.admin.operation;

import com.example.food.admin.operation.dto.AdminOperationLogPageResponse;
import com.example.food.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/operation-logs")
public class AdminOperationLogController {

    private final AdminOperationLogService service;

    public AdminOperationLogController(AdminOperationLogService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<AdminOperationLogPageResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String result,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return ApiResponse.ok(service.list(keyword, result, limit, offset));
    }
}
