CREATE TABLE user_kitchen_character_names (
    user_id BIGINT PRIMARY KEY,
    names_json TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_kitchen_character_names_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
