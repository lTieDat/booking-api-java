CREATE TABLE refunds (
    id                    UUID PRIMARY KEY,
    payment_id            UUID NOT NULL,
    amount_minor          BIGINT NOT NULL,
    currency              VARCHAR(3) NOT NULL DEFAULT 'VND',
    status                VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    provider_refund_id    VARCHAR(150),
    reason                VARCHAR(300),
    requested_by          UUID,
    requested_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at          TIMESTAMP,
    raw_payload           TEXT,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT chk_refunds_status CHECK (status IN ('REQUESTED', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_refunds_amount_positive CHECK (amount_minor > 0)
);

CREATE INDEX idx_refunds_payment_id ON refunds (payment_id);
CREATE INDEX idx_refunds_status ON refunds (status);
CREATE INDEX idx_refunds_requested_by ON refunds (requested_by);
CREATE UNIQUE INDEX uk_refunds_payment_provider_refund_id
    ON refunds (payment_id, provider_refund_id);
