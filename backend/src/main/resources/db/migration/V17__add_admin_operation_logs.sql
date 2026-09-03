CREATE TABLE admin_operation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT,
    admin_username VARCHAR(64) NOT NULL,
    operation_type VARCHAR(64) NOT NULL,
    http_method VARCHAR(16) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    operation_result VARCHAR(16) NOT NULL,
    status_code INT NOT NULL,
    ip_address VARCHAR(64),
    details VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_operation_logs_admin FOREIGN KEY (admin_id) REFERENCES admins(id) ON DELETE SET NULL
);

CREATE INDEX idx_admin_operation_logs_created_at ON admin_operation_logs(created_at);
CREATE INDEX idx_admin_operation_logs_admin_id ON admin_operation_logs(admin_id);
CREATE INDEX idx_admin_operation_logs_result ON admin_operation_logs(operation_result);
