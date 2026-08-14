package com.example.food.stats.image;

import com.example.food.stats.IngredientNormalizer;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/ingredients/images")
public class IngredientImageController {

    private final IngredientImageService imageService;
    private final IngredientNormalizer ingredientNormalizer;

    public IngredientImageController(
            IngredientImageService imageService,
            IngredientNormalizer ingredientNormalizer
    ) {
        this.imageService = imageService;
        this.ingredientNormalizer = ingredientNormalizer;
    }

    @GetMapping("/{name:.+}")
    public ResponseEntity<byte[]> image(@PathVariable String name) {
        String canonicalName = ingredientNormalizer.normalizeDistinct(name).stream()
                .findFirst()
                .map(IngredientNormalizer.NormalizedIngredient::canonicalName)
                .orElse(name.trim());
        IngredientImage image = imageService.findReady(canonicalName);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(image.getContentType());
        } catch (IllegalArgumentException exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(24)).cachePublic())
                .contentType(mediaType)
                .body(image.getImageData());
    }
}
