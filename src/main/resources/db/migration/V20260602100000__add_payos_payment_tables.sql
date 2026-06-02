CREATE TABLE payment_provider_accounts (
    id                     UUID PRIMARY KEY,
    provider               VARCHAR(30) NOT NULL,
    mode                   VARCHAR(10) NOT NULL DEFAULT 'TEST',
    display_name           VARCHAR(100) NOT NULL,
    merchant_code          VARCHAR(100),
    provider_channel_id    VARCHAR(100),
    webhook_url            VARCHAR(500),
    webhook_confirmed_at   TIMESTAMP,
    is_active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_provider_accounts_provider_mode_merchant UNIQUE (provider, mode, merchant_code),
    CONSTRAINT chk_payment_provider_accounts_provider CHECK (provider IN ('PAYOS'))
);

CREATE TABLE payments (
    id                    UUID PRIMARY KEY,
    booking_id            UUID NOT NULL,
    provider_account_id   UUID,
    provider              VARCHAR(30) NOT NULL,
    status                VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    provider_status       VARCHAR(40),
    amount_minor          BIGINT NOT NULL,
    currency              VARCHAR(3) NOT NULL DEFAULT 'VND',
    attempt_no            INT NOT NULL DEFAULT 1,
    provider_order_code   VARCHAR(100),
    provider_payment_id   VARCHAR(150),
    provider_reference    VARCHAR(150),
    checkout_url          VARCHAR(500),
    qr_code               TEXT,
    return_url            VARCHAR(500),
    cancel_url            VARCHAR(500),
    expires_at            TIMESTAMP,
    paid_at               TIMESTAMP,
    cancelled_at          TIMESTAMP,
    last_reconciled_at    TIMESTAMP,
    failure_reason        VARCHAR(500),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_payments_provider_account FOREIGN KEY (provider_account_id) REFERENCES payment_provider_accounts (id),
    CONSTRAINT uk_payments_booking_attempt UNIQUE (booking_id, attempt_no),
    CONSTRAINT uk_payments_provider_order_code UNIQUE (provider, provider_order_code),
    CONSTRAINT uk_payments_provider_payment_id UNIQUE (provider, provider_payment_id),
    CONSTRAINT chk_payments_provider CHECK (provider IN ('PAYOS')),
    CONSTRAINT chk_payments_status CHECK (status IN ('INITIATED', 'PENDING', 'PAID', 'PARTIALLY_REFUNDED', 'REFUNDED', 'CANCELLED', 'EXPIRED', 'FAILED')),
    CONSTRAINT chk_payments_amount_positive CHECK (amount_minor > 0)
);

CREATE INDEX idx_payments_booking_id ON payments (booking_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_expires_at ON payments (expires_at);

CREATE TABLE payment_transactions (
    id                        UUID PRIMARY KEY,
    payment_id                UUID NOT NULL,
    provider                  VARCHAR(30) NOT NULL,
    provider_transaction_id   VARCHAR(150),
    provider_order_code       VARCHAR(100),
    provider_payment_id       VARCHAR(150),
    amount_minor              BIGINT NOT NULL,
    currency                  VARCHAR(3) NOT NULL DEFAULT 'VND',
    description               VARCHAR(500),
    transaction_at            TIMESTAMP,
    raw_payload               TEXT NOT NULL,
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_transactions_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT uk_payment_transactions_provider_transaction UNIQUE (provider, provider_transaction_id),
    CONSTRAINT chk_payment_transactions_provider CHECK (provider IN ('PAYOS')),
    CONSTRAINT chk_payment_transactions_amount_positive CHECK (amount_minor > 0)
);

CREATE INDEX idx_payment_transactions_payment_id ON payment_transactions (payment_id);

CREATE TABLE payment_webhook_events (
    id                    UUID PRIMARY KEY,
    provider              VARCHAR(30) NOT NULL,
    payment_id            UUID,
    provider_event_id     VARCHAR(150),
    provider_order_code   VARCHAR(100),
    provider_payment_id   VARCHAR(150),
    event_type            VARCHAR(80),
    signature             VARCHAR(500),
    verified_at           TIMESTAMP,
    payload_hash          VARCHAR(64) NOT NULL,
    payload               TEXT NOT NULL,
    status                VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    received_at           TIMESTAMP NOT NULL,
    processed_at          TIMESTAMP,
    retry_count           INT NOT NULL DEFAULT 0,
    error_message         VARCHAR(1000),
    CONSTRAINT fk_payment_webhook_events_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT uk_payment_webhook_events_provider_payload_hash UNIQUE (provider, payload_hash),
    CONSTRAINT uk_payment_webhook_events_provider_event UNIQUE (provider, provider_event_id),
    CONSTRAINT chk_payment_webhook_events_provider CHECK (provider IN ('PAYOS')),
    CONSTRAINT chk_payment_webhook_events_status CHECK (status IN ('RECEIVED', 'PROCESSED', 'IGNORED', 'FAILED')),
    CONSTRAINT chk_payment_webhook_events_retry_count_non_negative CHECK (retry_count >= 0)
);

CREATE INDEX idx_payment_webhook_events_payment_id ON payment_webhook_events (payment_id);
CREATE INDEX idx_payment_webhook_events_status ON payment_webhook_events (status);
CREATE INDEX idx_payment_webhook_events_provider_order_code ON payment_webhook_events (provider_order_code);
CREATE INDEX idx_payment_webhook_events_provider_payment_id ON payment_webhook_events (provider_payment_id);

ALTER TABLE inventory_holds
    ADD CONSTRAINT fk_inventory_holds_payment FOREIGN KEY (payment_id) REFERENCES payments (id);
