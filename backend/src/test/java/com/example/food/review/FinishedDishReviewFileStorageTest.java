package com.example.food.review;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinishedDishReviewFileStorageTest {

    @TempDir
    Path tempDirectory;

    @Test
    void storesReadsAndDeletesSupportedImageFiles() throws Exception {
        FinishedDishReviewFileStorage storage = new FinishedDishReviewFileStorage(tempDirectory.toString());
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "dish.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        FinishedDishReviewFileStorage.UploadedImage image = storage.readAndValidate(file);
        FinishedDishReviewFileStorage.StoredFile stored = storage.store(image);

        assertThat(Files.isRegularFile(Path.of(stored.storagePath()))).isTrue();
        assertThat(storage.load(stored.storedName())).containsExactly(1, 2, 3);

        storage.deleteQuietly(stored.storedName());

        assertThat(Files.exists(Path.of(stored.storagePath()))).isFalse();
    }

    @Test
    void rejectsUnsupportedImageContentType() {
        FinishedDishReviewFileStorage storage = new FinishedDishReviewFileStorage(tempDirectory.toString());
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "dish.gif",
                "image/gif",
                new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> storage.readAndValidate(file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("仅支持 JPG、PNG、WebP");
    }
}
