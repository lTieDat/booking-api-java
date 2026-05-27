ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_date_range
        CHECK (check_out_date > check_in_date);

ALTER TABLE room_types
    ADD CONSTRAINT chk_room_types_base_price_non_negative
        CHECK (base_price >= 0);

ALTER TABLE room_types
    ADD CONSTRAINT chk_room_types_occupancy_positive
        CHECK (max_occupancy > 0);
