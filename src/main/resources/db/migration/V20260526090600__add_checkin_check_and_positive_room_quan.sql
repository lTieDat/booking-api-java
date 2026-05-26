ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_date_range
        CHECK (check_out_date > check_in_date);

ALTER TABLE rooms
    ADD CONSTRAINT chk_rooms_price_non_negative
        CHECK (price_per_night >= 0);

ALTER TABLE rooms
    ADD CONSTRAINT chk_rooms_capacity_positive
        CHECK (capacity > 0);
