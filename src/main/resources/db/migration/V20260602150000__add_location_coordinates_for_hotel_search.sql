ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS latitude NUMERIC(9, 6);

ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS longitude NUMERIC(9, 6);

UPDATE locations
SET latitude = 16.054407,
    longitude = 108.202167
WHERE id = CAST('50000000-0000-0000-0000-000000000001' AS UUID)
  AND latitude IS NULL
  AND longitude IS NULL;

UPDATE locations
SET latitude = 10.776889,
    longitude = 106.700806
WHERE id = CAST('50000000-0000-0000-0000-000000000002' AS UUID)
  AND latitude IS NULL
  AND longitude IS NULL;

CREATE INDEX IF NOT EXISTS idx_locations_latitude_longitude
    ON locations (latitude, longitude);
