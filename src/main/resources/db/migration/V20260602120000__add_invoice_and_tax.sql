CREATE TABLE tax_configs (
    id              UUID PRIMARY KEY,
    hotel_id        UUID,
    name            VARCHAR(100) NOT NULL,
    apply_type      VARCHAR(40) NOT NULL,
    rate            NUMERIC(8, 4),
    amount_minor    BIGINT,
    is_inclusive    BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tax_configs_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT chk_tax_configs_apply_type CHECK (apply_type IN ('PERCENTAGE', 'FIXED_PER_BOOKING', 'PER_ROOM_PER_NIGHT')),
    CONSTRAINT chk_tax_configs_amount_non_negative CHECK (amount_minor IS NULL OR amount_minor >= 0),
    CONSTRAINT chk_tax_configs_rate_non_negative CHECK (rate IS NULL OR rate >= 0)
);

CREATE TABLE invoices (
    id                UUID PRIMARY KEY,
    booking_id        UUID NOT NULL,
    payment_id        UUID,
    invoice_no        VARCHAR(50) NOT NULL,
    status            VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    subtotal_minor    BIGINT NOT NULL,
    discount_minor    BIGINT NOT NULL DEFAULT 0,
    tax_minor         BIGINT NOT NULL DEFAULT 0,
    total_minor       BIGINT NOT NULL,
    currency          VARCHAR(3) NOT NULL DEFAULT 'VND',
    issued_at         TIMESTAMP,
    paid_at           TIMESTAMP,
    voided_at         TIMESTAMP,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoices_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_invoices_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT uk_invoices_invoice_no UNIQUE (invoice_no),
    CONSTRAINT uk_invoices_booking_payment UNIQUE (booking_id, payment_id),
    CONSTRAINT chk_invoices_status CHECK (status IN ('DRAFT', 'ISSUED', 'PAID', 'VOIDED')),
    CONSTRAINT chk_invoices_amounts_non_negative
        CHECK (subtotal_minor >= 0 AND discount_minor >= 0 AND tax_minor >= 0 AND total_minor >= 0)
);

CREATE TABLE invoice_lines (
    id              UUID PRIMARY KEY,
    invoice_id      UUID NOT NULL,
    line_type       VARCHAR(40) NOT NULL,
    description     VARCHAR(300) NOT NULL,
    quantity        INT NOT NULL DEFAULT 1,
    unit_minor      BIGINT NOT NULL,
    total_minor     BIGINT NOT NULL,
    metadata        TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_lines_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    CONSTRAINT chk_invoice_lines_type CHECK (line_type IN ('ROOM', 'AMENITY', 'SERVICE', 'DISCOUNT', 'TAX', 'FEE')),
    CONSTRAINT chk_invoice_lines_quantity_positive CHECK (quantity > 0)
);

CREATE TABLE booking_taxes (
    id              UUID PRIMARY KEY,
    booking_id      UUID NOT NULL,
    tax_config_id   UUID,
    tax_name        VARCHAR(100) NOT NULL,
    apply_type      VARCHAR(40) NOT NULL,
    rate            NUMERIC(8, 4),
    amount_minor    BIGINT NOT NULL,
    is_inclusive    BOOLEAN NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_taxes_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_booking_taxes_tax_config FOREIGN KEY (tax_config_id) REFERENCES tax_configs (id),
    CONSTRAINT chk_booking_taxes_apply_type CHECK (apply_type IN ('PERCENTAGE', 'FIXED_PER_BOOKING', 'PER_ROOM_PER_NIGHT')),
    CONSTRAINT chk_booking_taxes_amount_non_negative CHECK (amount_minor >= 0)
);

CREATE INDEX idx_tax_configs_hotel_active ON tax_configs (hotel_id, is_active);
CREATE INDEX idx_invoices_booking_id ON invoices (booking_id);
CREATE INDEX idx_invoices_payment_id ON invoices (payment_id);
CREATE INDEX idx_invoices_status ON invoices (status);
CREATE INDEX idx_invoice_lines_invoice_id ON invoice_lines (invoice_id);
CREATE INDEX idx_booking_taxes_booking_id ON booking_taxes (booking_id);
