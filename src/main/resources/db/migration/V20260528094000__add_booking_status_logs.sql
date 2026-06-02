CREATE TABLE booking_status_logs (
    id                UUID PRIMARY KEY,
    booking_id        UUID         NOT NULL,
    from_status       VARCHAR(30),
    to_status         VARCHAR(30)  NOT NULL,
    performed_by      UUID,
    performed_by_type VARCHAR(30)  NOT NULL DEFAULT 'SYSTEM',
    note              VARCHAR(500),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_status_logs_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT chk_booking_status_logs_performed_by_type
        CHECK (performed_by_type IN ('USER', 'MANAGER', 'SYSTEM'))
);

CREATE INDEX idx_booking_status_logs_booking_id ON booking_status_logs (booking_id);
CREATE INDEX idx_booking_status_logs_performed_by ON booking_status_logs (performed_by);
CREATE INDEX idx_booking_status_logs_status_created_at ON booking_status_logs (to_status, created_at);
