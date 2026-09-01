ALTER TABLE search_logs ADD COLUMN result_title VARCHAR(128) NULL;
ALTER TABLE search_logs ADD COLUMN result_ingredients TEXT NULL;

CREATE TABLE recommendation_feedbacks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    search_log_id BIGINT NOT NULL,
    reaction VARCHAR(16),
    cooked TINYINT NOT NULL DEFAULT 0,
    reacted_at DATETIME,
    cooked_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_recommendation_feedback_user_log UNIQUE (user_id, search_log_id),
    CONSTRAINT fk_recommendation_feedback_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_recommendation_feedback_search_log FOREIGN KEY (search_log_id) REFERENCES search_logs(id) ON DELETE CASCADE
);

CREATE INDEX idx_recommendation_feedback_user_updated
    ON recommendation_feedbacks(user_id, updated_at);
