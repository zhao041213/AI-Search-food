package com.example.food.user.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAvatarFileStorageTest {
    @TempDir
    Path tempDir;

    @Test
    void acceptsRealPngSignatureAndStoresUnpredictableName() {
        UserAvatarFileStorage storage = new UserAvatarFileStorage(tempDir.toString());
        byte[] pngHeader = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00
        };

        UserAvatarFileStorage.StoredAvatar stored = storage.store(new MockMultipartFile(
                "image", "avatar.txt", "text/plain", pngHeader
        ));

        assertThat(stored.contentType()).isEqualTo("image/png");
        assertThat(stored.storedName()).endsWith(".png").doesNotContain("avatar");
        assertThat(storage.load(stored.storedName())).containsExactly(pngHeader);
        storage.deleteQuietly(stored.storedName());
        assertThatThrownBy(() -> storage.load(stored.storedName()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnsupportedContentRegardlessOfDeclaredExtension() {
        UserAvatarFileStorage storage = new UserAvatarFileStorage(tempDir.toString());

        assertThatThrownBy(() -> storage.store(new MockMultipartFile(
                "image", "avatar.jpg", "image/jpeg", "not-an-image".getBytes()
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("头像仅支持 JPG、PNG 或 WebP 图片");
    }
}
