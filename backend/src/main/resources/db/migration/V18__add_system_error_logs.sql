CREATE TABLE system_error_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_type VARCHAR(32) NOT NULL,
    severity VARCHAR(16) NOT NULL DEFAULT 'ERROR',
    component VARCHAR(128) NOT NULL,
    exception_class VARCHAR(255),
    message VARCHAR(1000),
    root_cause VARCHAR(1000),
    request_method VARCHAR(16),
    request_path VARCHAR(255),
    status_code INT,
    user_id BIGINT,
    admin_id BIGINT,
    ip_address VARCHAR(64),
    stack_trace TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_system_error_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_system_error_logs_admin FOREIGN KEY (admin_id) REFERENCES admins(id) ON DELETE SET NULL
);

CREATE INDEX idx_system_error_logs_created_at ON system_error_logs(created_at);
CREATE INDEX idx_system_error_logs_source_type ON system_error_logs(source_type);
CREATE INDEX idx_system_error_logs_status_code ON system_error_logs(status_code);
