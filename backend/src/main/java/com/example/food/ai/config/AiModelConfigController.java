package com.example.food.ai.config;

import com.example.food.ai.config.dto.AiModelConfigResponse;
import com.example.food.ai.config.dto.AiModelConfigUpdateRequest;
import com.example.food.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai-config")
public class AiModelConfigController {

    private final AiModelConfigService aiModelConfigService;

    public AiModelConfigController(AiModelConfigService aiModelConfigService) {
        this.aiModelConfigService = aiModelConfigService;
    }

    @GetMapping("/text-recipe")
    public ApiResponse<AiModelConfigResponse> textRecipeConfig() {
        return ApiResponse.ok(aiModelConfigService.textRecipeConfig());
    }

    @PutMapping("/text-recipe")
    public ApiResponse<AiModelConfigResponse> saveTextRecipeConfig(
            @Valid @RequestBody AiModelConfigUpdateRequest request
    ) {
        return ApiResponse.ok(aiModelConfigService.saveTextRecipeConfig(request));
    }
}
