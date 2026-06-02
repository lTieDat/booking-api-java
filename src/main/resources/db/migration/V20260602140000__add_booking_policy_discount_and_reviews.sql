ALTER TABLE discounts
    ADD COLUMN discount_value BIGINT NOT NULL DEFAULT 0;

ALTER TABLE cancellation_policies
    ADD COLUMN free_cancellation_hours INT NOT NULL DEFAULT 24;

ALTER TABLE cancellation_policies
    ADD COLUMN penalty_type VARCHAR(30) NOT NULL DEFAULT 'NONE';

ALTER TABLE cancellation_policies
    ADD COLUMN penalty_value BIGINT NOT NULL DEFAULT 0;

ALTER TABLE bookings
    ADD COLUMN discount_code_snapshot VARCHAR(20);

ALTER TABLE bookings
    ADD COLUMN discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0;

ALTER TABLE bookings
    ADD COLUMN cancellation_fee NUMERIC(10, 2) NOT NULL DEFAULT 0;

CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    user_id UUID NOT NULL,
    hotel_id UUID NOT NULL,
    rating INT NOT NULL,
    title VARCHAR(100),
    comment VARCHAR(1000),
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT uk_reviews_booking UNIQUE (booking_id),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_reviews_hotel_visible
    ON reviews (hotel_id, is_visible);

CREATE INDEX idx_reviews_user
    ON reviews (user_id);
