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
SELECT
    CAST('50000000-0000-0000-0000-000000000001' AS UUID),
    'Vietnam',
    'Da Nang',
    NULL,
    'Son Tra',
    '88 Vo Nguyen Giap Street',
    TIMESTAMP WITH TIME ZONE '2026-05-25 00:00:00+00:00',
    TIMESTAMP WITH TIME ZONE '2026-05-25 00:00:00+00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM locations WHERE id = CAST('50000000-0000-0000-0000-000000000001' AS UUID)
);

UPDATE locations
SET district = 'Son Tra'
WHERE id = CAST('50000000-0000-0000-0000-000000000001' AS UUID)
  AND district IS NULL;

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
SELECT
    CAST('50000000-0000-0000-0000-000000000002' AS UUID),
    'Vietnam',
    'Ho Chi Minh City',
    NULL,
    'District 1',
    '123 Nguyen Hue Street',
    TIMESTAMP WITH TIME ZONE '2026-05-25 00:00:00+00:00',
    TIMESTAMP WITH TIME ZONE '2026-05-25 00:00:00+00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM locations WHERE id = CAST('50000000-0000-0000-0000-000000000002' AS UUID)
);

UPDATE locations
SET district = 'District 1'
WHERE id = CAST('50000000-0000-0000-0000-000000000002' AS UUID)
  AND district IS NULL;

INSERT INTO hotels (
    id,
    name,
    description,
    location_id,
    created_by,
    updated_by,
    created_at,
    updated_at
)
SELECT
    CAST('30000000-0000-0000-0000-000000000001' AS UUID),
    'Grand Palace Hotel',
    'Central business hotel with premium rooms and meeting facilities.',
    CAST('50000000-0000-0000-0000-000000000002' AS UUID),
    CAST('20000000-0000-0000-0000-000000000001' AS UUID),
    CAST('20000000-0000-0000-0000-000000000001' AS UUID),
    TIMESTAMP WITH TIME ZONE '2026-05-25 00:00:00+00:00',
    TIMESTAMP WITH TIME ZONE '2026-05-25 00:00:00+00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM hotels WHERE id = CAST('30000000-0000-0000-0000-000000000001' AS UUID)
);

INSERT INTO hotels (
    id,
    name,
    description,
    location_id,
    created_by,
    updated_by,
    created_at,
    updated_at
)
SELECT
    CAST('30000000-0000-0000-0000-000000000002' AS UUID),
    'Seaside Resort',
    'Beachfront resort with family-friendly rooms and leisure services.',
    CAST('50000000-0000-0000-0000-000000000001' AS UUID),
    CAST('20000000-0000-0000-0000-000000000001' AS UUID),
    CAST('20000000-0000-0000-0000-000000000001' AS UUID),
    TIMESTAMP WITH TIME ZONE '2026-05-25 00:00:00+00:00',
    TIMESTAMP WITH TIME ZONE '2026-05-25 00:00:00+00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM hotels WHERE id = CAST('30000000-0000-0000-0000-000000000002' AS UUID)
);

INSERT INTO hotel_images (
    id,
    hotel_id,
    url,
    bucket,
    object_key,
    content_type,
    size_bytes,
    alt_text,
    image_type,
    created_at,
    updated_at
)
SELECT
    CAST('60000000-0000-0000-0000-000000000001' AS UUID),
    CAST('30000000-0000-0000-0000-000000000001' AS UUID),
    'http://localhost:9000/booking-local/seed/hotels/grand-palace/preview.jpg',
    'booking-local',
    'seed/hotels/grand-palace/preview.jpg',
    'image/jpeg',
    NULL,
    'Grand Palace Hotel preview',
    'PREVIEW',
    TIMESTAMP WITH TIME ZONE '2026-05-25 00:10:00+00:00',
    TIMESTAMP WITH TIME ZONE '2026-05-25 00:10:00+00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM hotel_images WHERE id = CAST('60000000-0000-0000-0000-000000000001' AS UUID)
);

INSERT INTO hotel_images (
    id,
    hotel_id,
    url,
    bucket,
    object_key,
    content_type,
    size_bytes,
    alt_text,
    image_type,
    created_at,
    updated_at
)
SELECT
    CAST('60000000-0000-0000-0000-000000000002' AS UUID),
    CAST('30000000-0000-0000-0000-000000000002' AS UUID),
    'http://localhost:9000/booking-local/seed/hotels/seaside-resort/preview.jpg',
    'booking-local',
    'seed/hotels/seaside-resort/preview.jpg',
    'image/jpeg',
    NULL,
    'Seaside Resort preview',
    'PREVIEW',
    TIMESTAMP WITH TIME ZONE '2026-05-25 00:10:00+00:00',
    TIMESTAMP WITH TIME ZONE '2026-05-25 00:10:00+00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM hotel_images WHERE id = CAST('60000000-0000-0000-0000-000000000002' AS UUID)
);

INSERT INTO room_types (
    id,
    hotel_id,
    name,
    code,
    max_adults,
    max_children,
    max_occupancy,
    bed_type,
    description,
    base_price,
    created_by,
    updated_by,
    created_at,
    updated_at
)
SELECT
    room_type_data.id,
    room_type_data.hotel_id,
    room_type_data.name,
    room_type_data.code,
    room_type_data.max_adults,
    room_type_data.max_children,
    room_type_data.max_occupancy,
    room_type_data.bed_type,
    room_type_data.description,
    room_type_data.base_price,
    room_type_data.created_by,
    room_type_data.updated_by,
    room_type_data.created_at,
    room_type_data.updated_at
FROM (
    VALUES
        (CAST('41000000-0000-0000-0000-000000000001' AS UUID), CAST('30000000-0000-0000-0000-000000000001' AS UUID), 'Standard', 'STD', 2, 0, 2, 'DOUBLE', 'Standard room for two guests.', 850000.00, CAST('20000000-0000-0000-0000-000000000001' AS UUID), CAST('20000000-0000-0000-0000-000000000001' AS UUID), TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00', TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00'),
        (CAST('41000000-0000-0000-0000-000000000002' AS UUID), CAST('30000000-0000-0000-0000-000000000001' AS UUID), 'Deluxe', 'DLX', 2, 0, 2, 'DOUBLE', 'Deluxe room with city view.', 1250000.00, CAST('20000000-0000-0000-0000-000000000001' AS UUID), CAST('20000000-0000-0000-0000-000000000001' AS UUID), TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00', TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00'),
        (CAST('41000000-0000-0000-0000-000000000003' AS UUID), CAST('30000000-0000-0000-0000-000000000001' AS UUID), 'Suite', 'STE', 4, 0, 4, 'DOUBLE', 'Suite room for families or groups.', 2200000.00, CAST('20000000-0000-0000-0000-000000000001' AS UUID), CAST('20000000-0000-0000-0000-000000000001' AS UUID), TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00', TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00'),
        (CAST('41000000-0000-0000-0000-000000000004' AS UUID), CAST('30000000-0000-0000-0000-000000000002' AS UUID), 'Standard', 'STD', 2, 0, 2, 'DOUBLE', 'Standard resort room.', 950000.00, CAST('20000000-0000-0000-0000-000000000001' AS UUID), CAST('20000000-0000-0000-0000-000000000001' AS UUID), TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00', TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00'),
        (CAST('41000000-0000-0000-0000-000000000005' AS UUID), CAST('30000000-0000-0000-0000-000000000002' AS UUID), 'Deluxe', 'DLX', 3, 0, 3, 'DOUBLE', 'Deluxe beachfront room.', 1450000.00, CAST('20000000-0000-0000-0000-000000000001' AS UUID), CAST('20000000-0000-0000-0000-000000000001' AS UUID), TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00', TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00'),
        (CAST('41000000-0000-0000-0000-000000000006' AS UUID), CAST('30000000-0000-0000-0000-000000000002' AS UUID), 'Family', 'FAM', 4, 0, 4, 'DOUBLE', 'Family room near the beach.', 1950000.00, CAST('20000000-0000-0000-0000-000000000001' AS UUID), CAST('20000000-0000-0000-0000-000000000001' AS UUID), TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00', TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00')
) AS room_type_data (
    id,
    hotel_id,
    name,
    code,
    max_adults,
    max_children,
    max_occupancy,
    bed_type,
    description,
    base_price,
    created_by,
    updated_by,
    created_at,
    updated_at
)
WHERE NOT EXISTS (
    SELECT 1 FROM room_types WHERE room_types.id = room_type_data.id
);

INSERT INTO rooms (
    id,
    room_type_id,
    room_number,
    floor,
    status,
    is_active,
    created_by,
    updated_by,
    created_at,
    updated_at
)
SELECT
    room_data.id,
    room_data.room_type_id,
    room_data.room_number,
    room_data.floor,
    room_data.status,
    room_data.is_active,
    room_data.created_by,
    room_data.updated_by,
    room_data.created_at,
    room_data.updated_at
FROM (
    VALUES
        (CAST('40000000-0000-0000-0000-000000000001' AS UUID), CAST('41000000-0000-0000-0000-000000000001' AS UUID), '101', 1, 'AVAILABLE', TRUE, CAST('20000000-0000-0000-0000-000000000001' AS UUID), CAST('20000000-0000-0000-0000-000000000001' AS UUID), TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00', TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00'),
        (CAST('40000000-0000-0000-0000-000000000002' AS UUID), CAST('41000000-0000-0000-0000-000000000002' AS UUID), '102', 1, 'AVAILABLE', TRUE, CAST('20000000-0000-0000-0000-000000000001' AS UUID), CAST('20000000-0000-0000-0000-000000000001' AS UUID), TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00', TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00'),
        (CAST('40000000-0000-0000-0000-000000000003' AS UUID), CAST('41000000-0000-0000-0000-000000000003' AS UUID), '201', 2, 'AVAILABLE', TRUE, CAST('20000000-0000-0000-0000-000000000001' AS UUID), CAST('20000000-0000-0000-0000-000000000001' AS UUID), TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00', TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00'),
        (CAST('40000000-0000-0000-0000-000000000004' AS UUID), CAST('41000000-0000-0000-0000-000000000004' AS UUID), 'A01', 1, 'AVAILABLE', TRUE, CAST('20000000-0000-0000-0000-000000000001' AS UUID), CAST('20000000-0000-0000-0000-000000000001' AS UUID), TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00', TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00'),
        (CAST('40000000-0000-0000-0000-000000000005' AS UUID), CAST('41000000-0000-0000-0000-000000000005' AS UUID), 'A02', 1, 'AVAILABLE', TRUE, CAST('20000000-0000-0000-0000-000000000001' AS UUID), CAST('20000000-0000-0000-0000-000000000001' AS UUID), TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00', TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00'),
        (CAST('40000000-0000-0000-0000-000000000006' AS UUID), CAST('41000000-0000-0000-0000-000000000006' AS UUID), 'B01', 2, 'AVAILABLE', TRUE, CAST('20000000-0000-0000-0000-000000000001' AS UUID), CAST('20000000-0000-0000-0000-000000000001' AS UUID), TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00', TIMESTAMP WITH TIME ZONE '2026-05-25 00:05:00+00:00')
) AS room_data (
    id,
    room_type_id,
    room_number,
    floor,
    status,
    is_active,
    created_by,
    updated_by,
    created_at,
    updated_at
)
WHERE NOT EXISTS (
    SELECT 1 FROM rooms WHERE rooms.id = room_data.id
);
