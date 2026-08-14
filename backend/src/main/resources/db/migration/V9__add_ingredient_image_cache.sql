CREATE TABLE ingredient_images (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    canonical_name VARCHAR(128) NOT NULL,
    image_data LONGBLOB,
    content_type VARCHAR(64),
    source_provider VARCHAR(64),
    source_url VARCHAR(1024),
    verification_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    verification_score DECIMAL(5,4),
    failure_reason VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_ingredient_images_canonical_name UNIQUE (canonical_name)
);

CREATE INDEX idx_ingredient_images_status_updated_at
    ON ingredient_images(verification_status, updated_at);
