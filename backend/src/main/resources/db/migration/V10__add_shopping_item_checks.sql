CREATE TABLE shopping_item_checks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    search_log_id BIGINT NOT NULL,
    ingredient_name VARCHAR(128) NOT NULL,
    checked TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_shopping_item_checks_user_search_ingredient
        UNIQUE (user_id, search_log_id, ingredient_name),
    CONSTRAINT fk_shopping_item_checks_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_shopping_item_checks_search_log
        FOREIGN KEY (search_log_id) REFERENCES search_logs(id) ON DELETE CASCADE
);

CREATE INDEX idx_shopping_item_checks_user_search
    ON shopping_item_checks(user_id, search_log_id);
