CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(32) NOT NULL UNIQUE,
    nickname VARCHAR(64) NOT NULL,
    avatar_url VARCHAR(512),
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at DATETIME
);

CREATE TABLE admins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'ADMIN',
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    preference_type VARCHAR(64) NOT NULL,
    preference_value VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE user_pantry_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    ingredient_name VARCHAR(128) NOT NULL,
    category VARCHAR(64),
    quantity DECIMAL(10, 2),
    unit VARCHAR(32),
    expire_date DATE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_pantry_items_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE search_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    anonymous_id VARCHAR(64),
    query_text VARCHAR(1000),
    input_type VARCHAR(32) NOT NULL,
    recognized_ingredients TEXT,
    ai_model VARCHAR(128),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_search_logs_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE recipe_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    search_log_id BIGINT,
    parent_recipe_id BIGINT,
    title VARCHAR(128) NOT NULL,
    difficulty VARCHAR(64),
    cooking_time VARCHAR(64),
    taste VARCHAR(128),
    servings INT,
    reason TEXT,
    nutrition_advice TEXT,
    video_keywords TEXT,
    ai_model VARCHAR(128),
    raw_response MEDIUMTEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recipe_records_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_recipe_records_search_log FOREIGN KEY (search_log_id) REFERENCES search_logs(id),
    CONSTRAINT fk_recipe_records_parent FOREIGN KEY (parent_recipe_id) REFERENCES recipe_records(id)
);

CREATE TABLE recipe_ingredients (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    ingredient_name VARCHAR(128) NOT NULL,
    amount VARCHAR(128),
    category VARCHAR(64),
    already_owned TINYINT NOT NULL DEFAULT 0,
    substitute_names VARCHAR(512),
    CONSTRAINT fk_recipe_ingredients_recipe FOREIGN KEY (recipe_id) REFERENCES recipe_records(id)
);

CREATE TABLE recipe_steps (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    step_no INT NOT NULL,
    title VARCHAR(128),
    instruction TEXT NOT NULL,
    estimated_minutes INT,
    tip TEXT,
    CONSTRAINT fk_recipe_steps_recipe FOREIGN KEY (recipe_id) REFERENCES recipe_records(id)
);

CREATE TABLE shopping_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    ingredient_name VARCHAR(128) NOT NULL,
    amount VARCHAR(128),
    category VARCHAR(64),
    already_owned TINYINT NOT NULL DEFAULT 0,
    recommended_buy TINYINT NOT NULL DEFAULT 1,
    substitute_names VARCHAR(512),
    taobao_url VARCHAR(1000),
    jd_url VARCHAR(1000),
    CONSTRAINT fk_shopping_items_recipe FOREIGN KEY (recipe_id) REFERENCES recipe_records(id)
);

CREATE TABLE recipe_favorites (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    recipe_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_recipe_favorites_user_recipe UNIQUE (user_id, recipe_id),
    CONSTRAINT fk_recipe_favorites_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_recipe_favorites_recipe FOREIGN KEY (recipe_id) REFERENCES recipe_records(id)
);

CREATE TABLE uploaded_files (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    purpose VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_uploaded_files_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE finished_dish_reviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    recipe_id BIGINT,
    uploaded_file_id BIGINT NOT NULL,
    review_result TEXT NOT NULL,
    ai_model VARCHAR(128),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_finished_dish_reviews_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_finished_dish_reviews_recipe FOREIGN KEY (recipe_id) REFERENCES recipe_records(id),
    CONSTRAINT fk_finished_dish_reviews_file FOREIGN KEY (uploaded_file_id) REFERENCES uploaded_files(id)
);

CREATE TABLE prompt_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_key VARCHAR(128) NOT NULL UNIQUE,
    template_name VARCHAR(128) NOT NULL,
    content TEXT NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE ai_model_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider VARCHAR(64) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    purpose VARCHAR(64) NOT NULL,
    primary_model TINYINT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_search_logs_created_at ON search_logs(created_at);
CREATE INDEX idx_search_logs_user_id ON search_logs(user_id);
CREATE INDEX idx_recipe_records_created_at ON recipe_records(created_at);
CREATE INDEX idx_recipe_records_user_id ON recipe_records(user_id);
CREATE INDEX idx_user_pantry_items_user_id ON user_pantry_items(user_id);
