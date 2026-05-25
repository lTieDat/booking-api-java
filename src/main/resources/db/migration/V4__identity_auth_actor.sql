ALTER TABLE users
    ADD COLUMN is_verified BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE managers (
    id            UUID PRIMARY KEY,
    email         VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_managers_email UNIQUE (email)
);

CREATE TABLE otp_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL,
    token      VARCHAR(10) NOT NULL,
    purpose    VARCHAR(40) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_used    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_otp_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_otp_tokens_user_purpose_used ON otp_tokens (user_id, purpose, is_used);
CREATE INDEX idx_otp_tokens_used_expiry ON otp_tokens (is_used, expires_at);

INSERT INTO managers (id, email, password_hash, full_name, is_active, created_at, updated_at)
VALUES (
    '20000000-0000-0000-0000-000000000001',
    'admin@booking.local',
    '$2a$10$pm54kWo4kjcXsZgo73ccDO93gglSyNr0vIBUsI7ycp7XD1nQU07WW',
    'System Admin',
    TRUE,
    TIMESTAMP WITH TIME ZONE '2026-05-22 00:00:00+00:00',
    TIMESTAMP WITH TIME ZONE '2026-05-22 00:00:00+00:00'
);
