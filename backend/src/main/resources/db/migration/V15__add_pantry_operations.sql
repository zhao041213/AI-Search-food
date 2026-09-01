CREATE TABLE pantry_operations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    source_type VARCHAR(32),
    source_id BIGINT,
    idempotency_key VARCHAR(96) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'COMPLETED',
    actual_servings INT,
    reversed_operation_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reversed_at DATETIME,
    CONSTRAINT uq_pantry_operations_idempotency UNIQUE (user_id, operation_type, idempotency_key),
    CONSTRAINT fk_pantry_operations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_pantry_operations_reversed FOREIGN KEY (reversed_operation_id) REFERENCES pantry_operations(id) ON DELETE SET NULL
);

CREATE INDEX idx_pantry_operations_user_created ON pantry_operations(user_id, created_at DESC);

CREATE TABLE pantry_operation_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operation_id BIGINT NOT NULL,
    pantry_item_id BIGINT,
    ingredient_name VARCHAR(128) NOT NULL,
    quantity DECIMAL(10, 2) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    before_quantity DECIMAL(10, 2),
    after_quantity DECIMAL(10, 2),
    raw_amount VARCHAR(128),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pantry_operation_items_operation FOREIGN KEY (operation_id) REFERENCES pantry_operations(id) ON DELETE CASCADE,
    CONSTRAINT fk_pantry_operation_items_pantry FOREIGN KEY (pantry_item_id) REFERENCES user_pantry_items(id) ON DELETE SET NULL
);

CREATE INDEX idx_pantry_operation_items_operation ON pantry_operation_items(operation_id);
CREATE INDEX idx_pantry_operation_items_pantry ON pantry_operation_items(pantry_item_id);
