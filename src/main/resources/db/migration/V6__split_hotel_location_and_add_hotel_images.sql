CREATE TABLE locations (
    id         UUID PRIMARY KEY,
    country    VARCHAR(100) NOT NULL,
    city       VARCHAR(100) NOT NULL,
    province   VARCHAR(100),
    district   VARCHAR(100),
    detail     VARCHAR(250),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE hotels
    ADD COLUMN location_id UUID;

INSERT INTO locations (
    id,
    country,
    city,
    province,
    district,
    detail,
    created_at,
    updated_at
)
WITH distinct_locations AS (
    SELECT DISTINCT
        h.country,
        h.city,
        h.address
    FROM hotels h
    WHERE h.country IS NOT NULL
      AND h.city IS NOT NULL
),
numbered_locations AS (
    SELECT
        ROW_NUMBER() OVER (ORDER BY country, city, address) AS row_num,
        country,
        city,
        address
    FROM distinct_locations
)
SELECT
    CAST('50000000-0000-0000-0000-' || LPAD(CAST(row_num AS VARCHAR), 12, '0') AS UUID),
    country,
    city,
    NULL,
    NULL,
    address,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM numbered_locations;

UPDATE hotels h
SET location_id = l.id
FROM locations l
WHERE l.country = h.country
  AND l.city = h.city
  AND COALESCE(l.detail, '') = COALESCE(h.address, '');

ALTER TABLE hotels
    ALTER COLUMN location_id SET NOT NULL;

ALTER TABLE hotels
    ADD CONSTRAINT fk_hotels_location
        FOREIGN KEY (location_id) REFERENCES locations (id);

CREATE INDEX idx_hotels_location_id ON hotels (location_id);

ALTER TABLE hotels
    DROP COLUMN address;

ALTER TABLE hotels
    DROP COLUMN city;

ALTER TABLE hotels
    DROP COLUMN country;

CREATE TABLE hotel_images (
    id         UUID PRIMARY KEY,
    hotel_id   UUID NOT NULL,
    url        VARCHAR(500) NOT NULL,
    bucket     VARCHAR(100),
    object_key VARCHAR(500),
    content_type VARCHAR(100),
    size_bytes BIGINT,
    alt_text   VARCHAR(100) NOT NULL,
    image_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_hotel_images_image_type
        CHECK (image_type IN ('PREVIEW', 'GALLERY', 'THUMBNAIL')),
    CONSTRAINT fk_hotel_images_hotel
        FOREIGN KEY (hotel_id) REFERENCES hotels (id) ON DELETE CASCADE
);

CREATE INDEX idx_hotel_images_hotel_id ON hotel_images (hotel_id);
