CREATE TABLE user_health_profiles (
    user_id BIGINT PRIMARY KEY,
    age_range VARCHAR(24) NOT NULL,
    height_cm DECIMAL(5, 1) NOT NULL,
    weight_kg DECIMAL(5, 1) NOT NULL,
    activity_level VARCHAR(24) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_health_profiles_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
