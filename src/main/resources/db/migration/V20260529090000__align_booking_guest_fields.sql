ALTER TABLE guests
    ADD COLUMN IF NOT EXISTS email VARCHAR(100);

UPDATE guests
SET email = identify_card_no || '@guest.local'
WHERE email IS NULL;

ALTER TABLE guests
    ALTER COLUMN email SET NOT NULL;

ALTER TABLE guests
    ADD CONSTRAINT uk_guests_email UNIQUE (email);

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS check_in_date_time TIMESTAMP;

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS check_out_date_time TIMESTAMP;

UPDATE bookings
SET check_in_date_time = CAST(check_in_date AS TIMESTAMP)
WHERE check_in_date_time IS NULL;

UPDATE bookings
SET check_out_date_time = CAST(check_out_date AS TIMESTAMP)
WHERE check_out_date_time IS NULL;

ALTER TABLE bookings
    ALTER COLUMN check_in_date_time SET NOT NULL;

ALTER TABLE bookings
    ALTER COLUMN check_out_date_time SET NOT NULL;

ALTER TABLE bookings
    DROP CONSTRAINT IF EXISTS chk_bookings_date_range;

ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_date_time_range
        CHECK (check_out_date_time > check_in_date_time);

ALTER TABLE bookings
    DROP COLUMN IF EXISTS check_in_date;

ALTER TABLE bookings
    DROP COLUMN IF EXISTS check_out_date;
