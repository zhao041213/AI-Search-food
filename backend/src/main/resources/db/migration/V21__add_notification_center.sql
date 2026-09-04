CREATE TABLE user_notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    content TEXT,
    target_path VARCHAR(255),
    status VARCHAR(16) NOT NULL DEFAULT 'UNREAD',
    dedupe_key VARCHAR(255) NOT NULL,
    read_at DATETIME,
    archived_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_notifications_user_dedupe UNIQUE (user_id, dedupe_key),
    CONSTRAINT fk_user_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_notifications_user_status_created
    ON user_notifications(user_id, status, created_at DESC, id DESC);

CREATE TABLE user_notification_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    pantry_expiring_enabled TINYINT NOT NULL DEFAULT 1,
    pantry_expired_enabled TINYINT NOT NULL DEFAULT 1,
    weekly_menu_preparation_enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_notification_preferences_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE notification_scan_runs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    started_at DATETIME NOT NULL,
    finished_at DATETIME,
    status VARCHAR(24) NOT NULL,
    processed_count INT NOT NULL DEFAULT 0,
    error_count INT NOT NULL DEFAULT 0,
    error_summary VARCHAR(1000)
);

CREATE INDEX idx_notification_scan_runs_started
    ON notification_scan_runs(started_at DESC, id DESC);
