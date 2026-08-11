package com.example.food.stats;

import com.example.food.common.ApiResponse;
import com.example.food.stats.dto.HotIngredientStatsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class HotIngredientStatsController {

    private final HotIngredientStatsService hotIngredientStatsService;

    public HotIngredientStatsController(HotIngredientStatsService hotIngredientStatsService) {
        this.hotIngredientStatsService = hotIngredientStatsService;
    }

    @GetMapping("/hot-ingredients")
    public ApiResponse<HotIngredientStatsResponse> hotIngredients(
            @RequestParam(defaultValue = "all") String period,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.ok(hotIngredientStatsService.get(period, limit));
    }
}
