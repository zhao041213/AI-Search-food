CREATE TABLE IF NOT EXISTS agent_conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_conversations_user_updated (user_id, updated_at DESC, id DESC),
    CONSTRAINT fk_agent_conversations_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS agent_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    block_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_messages_conversation_created (conversation_id, created_at DESC, id DESC),
    CONSTRAINT fk_agent_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES agent_conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_messages_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS agent_confirmations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at DATETIME,
    INDEX idx_agent_confirmations_user_status (user_id, status, created_at DESC),
    CONSTRAINT uq_agent_confirmations_user_key UNIQUE (user_id, idempotency_key),
    CONSTRAINT fk_agent_confirmations_conversation
        FOREIGN KEY (conversation_id) REFERENCES agent_conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_confirmations_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
