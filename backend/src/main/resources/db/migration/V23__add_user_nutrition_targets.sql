CREATE TABLE user_nutrition_targets (
    user_id BIGINT PRIMARY KEY,
    enabled TINYINT NOT NULL DEFAULT 0,
    calories_kcal DECIMAL(8,2),
    protein_g DECIMAL(8,2),
    fat_g DECIMAL(8,2),
    carbohydrate_g DECIMAL(8,2),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_nutrition_targets_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_user_nutrition_targets_values CHECK (
        (enabled = 0
            AND calories_kcal IS NULL
            AND protein_g IS NULL
            AND fat_g IS NULL
            AND carbohydrate_g IS NULL)
        OR
        (enabled = 1
            AND calories_kcal IS NOT NULL AND calories_kcal > 0 AND calories_kcal <= 10000
            AND protein_g IS NOT NULL AND protein_g > 0 AND protein_g <= 1000
            AND fat_g IS NOT NULL AND fat_g > 0 AND fat_g <= 1000
            AND carbohydrate_g IS NOT NULL AND carbohydrate_g > 0 AND carbohydrate_g <= 1000)
    )
);
