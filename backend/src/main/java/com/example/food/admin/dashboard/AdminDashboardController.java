package com.example.food.admin.dashboard;

import com.example.food.admin.dashboard.dto.AdminDashboardOverviewResponse;
import com.example.food.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ApiResponse<AdminDashboardOverviewResponse> overview(
            @RequestParam(defaultValue = "7d") String period
    ) {
        return ApiResponse.ok(dashboardService.overview(period));
    }
}
