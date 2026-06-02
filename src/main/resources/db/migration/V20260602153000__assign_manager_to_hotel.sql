ALTER TABLE managers
    ADD COLUMN IF NOT EXISTS hotel_id UUID;

ALTER TABLE managers
    ADD CONSTRAINT fk_managers_hotel
        FOREIGN KEY (hotel_id) REFERENCES hotels (id);

CREATE INDEX IF NOT EXISTS idx_managers_hotel_id
    ON managers (hotel_id);

UPDATE managers
SET hotel_id = CAST('30000000-0000-0000-0000-000000000001' AS UUID)
WHERE email = 'manager@booking.local'
  AND hotel_id IS NULL;
