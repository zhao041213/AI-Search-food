CREATE TABLE recipe_collections (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    is_default TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_recipe_collections_user_name UNIQUE (user_id, name),
    CONSTRAINT fk_recipe_collections_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_recipe_collections_user_default
    ON recipe_collections(user_id, is_default);

CREATE TABLE recipe_collection_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    collection_id BIGINT NOT NULL,
    recipe_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_recipe_collection_items_user_recipe UNIQUE (user_id, recipe_id),
    CONSTRAINT fk_recipe_collection_items_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_recipe_collection_items_collection FOREIGN KEY (collection_id)
        REFERENCES recipe_collections(id) ON DELETE CASCADE,
    CONSTRAINT fk_recipe_collection_items_recipe FOREIGN KEY (recipe_id)
        REFERENCES recipe_records(id) ON DELETE CASCADE
);

CREATE INDEX idx_recipe_collection_items_collection
    ON recipe_collection_items(user_id, collection_id);

CREATE TABLE recipe_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_recipe_tags_user_name UNIQUE (user_id, name),
    CONSTRAINT fk_recipe_tags_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_recipe_tags_user ON recipe_tags(user_id);

CREATE TABLE recipe_record_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    recipe_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_recipe_record_tags_identity UNIQUE (user_id, recipe_id, tag_id),
    CONSTRAINT fk_recipe_record_tags_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_recipe_record_tags_recipe FOREIGN KEY (recipe_id)
        REFERENCES recipe_records(id) ON DELETE CASCADE,
    CONSTRAINT fk_recipe_record_tags_tag FOREIGN KEY (tag_id)
        REFERENCES recipe_tags(id) ON DELETE CASCADE
);

CREATE INDEX idx_recipe_record_tags_recipe
    ON recipe_record_tags(user_id, recipe_id);

CREATE TABLE recipe_shares (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    recipe_id BIGINT NULL,
    token CHAR(64) NOT NULL,
    validity_type VARCHAR(16) NOT NULL,
    expires_at DATETIME NULL,
    disabled_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_recipe_shares_token UNIQUE (token),
    CONSTRAINT fk_recipe_shares_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_recipe_shares_recipe FOREIGN KEY (recipe_id)
        REFERENCES recipe_records(id) ON DELETE SET NULL
);

CREATE INDEX idx_recipe_shares_user_created
    ON recipe_shares(user_id, created_at);

CREATE TABLE user_security_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    details VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_security_logs_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_security_logs_user_created
    ON user_security_logs(user_id, created_at);
