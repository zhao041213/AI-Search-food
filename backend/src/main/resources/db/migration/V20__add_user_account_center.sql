ALTER TABLE users ADD COLUMN auth_version INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN deleted_at DATETIME NULL;

CREATE TABLE user_security_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    event_type VARCHAR(64) NOT NULL,
    event_result VARCHAR(16) NOT NULL,
    ip_address VARCHAR(64),
    details VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_security_events_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_user_security_events_user_created
    ON user_security_events(user_id, created_at);
