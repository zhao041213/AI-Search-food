ALTER TABLE users ADD COLUMN password_hash VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN password_failed_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN password_locked_until DATETIME NULL;
