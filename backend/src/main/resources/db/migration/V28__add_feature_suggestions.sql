CREATE TABLE feature_suggestions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(80) NOT NULL,
    detail VARCHAR(2000) NOT NULL,
    expected_effect VARCHAR(500),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    screenshot_original_name VARCHAR(255),
    screenshot_stored_name VARCHAR(80),
    screenshot_content_type VARCHAR(64),
    screenshot_size BIGINT,
    screenshot_storage_path VARCHAR(500),
    duplicate_of_id BIGINT,
    internal_note VARCHAR(2000),
    admin_reply VARCHAR(2000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_processed_at DATETIME,
    deleted_at DATETIME,
    CONSTRAINT fk_feature_suggestions_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_feature_suggestions_duplicate
        FOREIGN KEY (duplicate_of_id) REFERENCES feature_suggestions(id) ON DELETE SET NULL
);

CREATE INDEX idx_feature_suggestions_user_created
    ON feature_suggestions(user_id, created_at DESC, id DESC);
CREATE INDEX idx_feature_suggestions_status_created
    ON feature_suggestions(status, created_at DESC, id DESC);
CREATE INDEX idx_feature_suggestions_deleted
    ON feature_suggestions(deleted_at, created_at DESC, id DESC);

CREATE TABLE feature_suggestion_additions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    suggestion_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feature_suggestion_additions_suggestion
        FOREIGN KEY (suggestion_id) REFERENCES feature_suggestions(id) ON DELETE CASCADE,
    CONSTRAINT fk_feature_suggestion_additions_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_feature_suggestion_additions_suggestion_created
    ON feature_suggestion_additions(suggestion_id, created_at ASC, id ASC);

CREATE TABLE feature_suggestion_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    suggestion_id BIGINT NOT NULL,
    operator_id BIGINT,
    operator_type VARCHAR(16) NOT NULL,
    action VARCHAR(40) NOT NULL,
    details VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feature_suggestion_audit_suggestion
        FOREIGN KEY (suggestion_id) REFERENCES feature_suggestions(id) ON DELETE CASCADE
);

CREATE INDEX idx_feature_suggestion_audit_suggestion_created
    ON feature_suggestion_audit_logs(suggestion_id, created_at DESC, id DESC);
