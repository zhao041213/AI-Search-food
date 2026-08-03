CREATE TABLE phone_verification_code_locks (
    phone VARCHAR(32) NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (phone, purpose)
);
