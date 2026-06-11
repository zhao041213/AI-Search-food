package com.example.food.ai.ingredient;

import com.example.food.ai.ingredient.dto.IngredientRecognitionResponse;
import com.example.food.common.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai/ingredients")
public class IngredientRecognitionController {

    private final IngredientRecognitionService ingredientRecognitionService;

    public IngredientRecognitionController(IngredientRecognitionService ingredientRecognitionService) {
        this.ingredientRecognitionService = ingredientRecognitionService;
    }

    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<IngredientRecognitionResponse> recognize(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(ingredientRecognitionService.recognize(file));
    }
}
