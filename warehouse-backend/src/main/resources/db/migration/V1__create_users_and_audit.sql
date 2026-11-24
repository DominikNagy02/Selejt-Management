-- V1__create_users_and_audit.sql
-- Users and Audit Events schema (FR-12, FR-13, NFR-03)

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'DISPATCHER', 'WORKER'))
);

CREATE TABLE audit_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NULL,
    action VARCHAR(30) NOT NULL,
    details VARCHAR(4000) NULL,
    performed_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_audit_events_performed_by FOREIGN KEY (performed_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_entity ON audit_events (entity_type, entity_id);
CREATE INDEX idx_audit_created ON audit_events (created_at);
