CREATE TABLE search_log_ingredients (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    search_log_id BIGINT NOT NULL,
    ingredient_name VARCHAR(128) NOT NULL,
    original_name VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_search_log_ingredients_log_name UNIQUE (search_log_id, ingredient_name),
    CONSTRAINT fk_search_log_ingredients_log FOREIGN KEY (search_log_id) REFERENCES search_logs(id) ON DELETE CASCADE
);

CREATE INDEX idx_search_log_ingredients_name_created_at
    ON search_log_ingredients(ingredient_name, created_at);
