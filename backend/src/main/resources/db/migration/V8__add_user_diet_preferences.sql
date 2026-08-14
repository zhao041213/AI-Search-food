CREATE TABLE user_diet_preferences (
    user_id BIGINT PRIMARY KEY,
    taste VARCHAR(32),
    default_goal VARCHAR(64),
    avoid_ingredients TEXT NOT NULL,
    allergen_ingredients TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_diet_preferences_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_search_logs_user_created
    ON search_logs(user_id, created_at);
