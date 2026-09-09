package com.example.food.agent;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentAttachmentTest {

    @Test
    void copiesSupportedImageIntoMemory() throws Exception {
        byte[] source = new byte[]{1, 2, 3};
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "food.png",
                "image/png",
                source
        );

        AgentAttachment attachment = AgentAttachment.from(file);
        source[0] = 9;

        assertThat(attachment).isNotNull();
        assertThat(attachment.originalFilename()).isEqualTo("food.png");
        assertThat(attachment.contentType()).isEqualTo("image/png");
        assertThat(attachment.bytes()).containsExactly(1, 2, 3);
        assertThat(attachment.asMultipartFile().getBytes()).containsExactly(1, 2, 3);
    }

    @Test
    void rejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "food.gif",
                "image/gif",
                new byte[]{1}
        );

        assertThatThrownBy(() -> AgentAttachment.from(file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    @Test
    void rejectsImageLargerThanFiveMegabytes() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "food.jpg",
                "image/jpeg",
                new byte[5 * 1024 * 1024 + 1]
        );

        assertThatThrownBy(() -> AgentAttachment.from(file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }
}
