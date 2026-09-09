package com.example.food.agent.dto;

import jakarta.validation.constraints.Size;

public record AgentChatRequest(
        Long conversationId,
        @Size(max = 1000)
        String message,
        Long confirmationId,
        @Size(max = 64)
        String idempotencyKey
) {
}
