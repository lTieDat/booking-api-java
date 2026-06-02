CREATE TABLE receptionist_assignments (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL,
    hotel_id   UUID NOT NULL,
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID,
    updated_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_receptionist_assignments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_receptionist_assignments_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT uk_receptionist_assignments_user_hotel UNIQUE (user_id, hotel_id)
);

CREATE INDEX idx_receptionist_assignments_hotel_active
    ON receptionist_assignments (hotel_id, is_active);

INSERT INTO managers (id, email, password_hash, full_name, is_active, created_at, updated_at)
SELECT
    CAST('20000000-0000-0000-0000-000000000002' AS UUID),
    'manager@booking.local',
    '$2a$10$pm54kWo4kjcXsZgo73ccDO93gglSyNr0vIBUsI7ycp7XD1nQU07WW',
    'Operations Manager',
    TRUE,
    TIMESTAMP WITH TIME ZONE '2026-05-28 00:00:00+00:00',
    TIMESTAMP WITH TIME ZONE '2026-05-28 00:00:00+00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM managers
    WHERE email = 'manager@booking.local'
);

INSERT INTO users (id, name, username, email, password, is_verified, created_at, updated_at)
SELECT
    CAST('11000000-0000-0000-0000-000000000001' AS UUID),
    'Front Desk',
    'reception',
    'reception@booking.local',
    '$2a$10$pm54kWo4kjcXsZgo73ccDO93gglSyNr0vIBUsI7ycp7XD1nQU07WW',
    TRUE,
    TIMESTAMP WITH TIME ZONE '2026-05-28 00:05:00+00:00',
    TIMESTAMP WITH TIME ZONE '2026-05-28 00:05:00+00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'reception@booking.local'
);

INSERT INTO users (id, name, username, email, password, is_verified, created_at, updated_at)
SELECT
    CAST('11000000-0000-0000-0000-000000000002' AS UUID),
    'Demo Guest',
    'demoguest',
    'guest@booking.local',
    '$2a$10$pm54kWo4kjcXsZgo73ccDO93gglSyNr0vIBUsI7ycp7XD1nQU07WW',
    TRUE,
    TIMESTAMP WITH TIME ZONE '2026-05-28 00:10:00+00:00',
    TIMESTAMP WITH TIME ZONE '2026-05-28 00:10:00+00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'guest@booking.local'
);

INSERT INTO user_roles (user_id, role_id)
SELECT
    CAST('11000000-0000-0000-0000-000000000001' AS UUID),
    CAST('10000000-0000-0000-0000-000000000001' AS UUID)
WHERE NOT EXISTS (
    SELECT 1
    FROM user_roles
    WHERE user_id = CAST('11000000-0000-0000-0000-000000000001' AS UUID)
      AND role_id = CAST('10000000-0000-0000-0000-000000000001' AS UUID)
);

INSERT INTO user_roles (user_id, role_id)
SELECT
    CAST('11000000-0000-0000-0000-000000000001' AS UUID),
    CAST('10000000-0000-0000-0000-000000000003' AS UUID)
WHERE NOT EXISTS (
    SELECT 1
    FROM user_roles
    WHERE user_id = CAST('11000000-0000-0000-0000-000000000001' AS UUID)
      AND role_id = CAST('10000000-0000-0000-0000-000000000003' AS UUID)
);

INSERT INTO user_roles (user_id, role_id)
SELECT
    CAST('11000000-0000-0000-0000-000000000002' AS UUID),
    CAST('10000000-0000-0000-0000-000000000001' AS UUID)
WHERE NOT EXISTS (
    SELECT 1
    FROM user_roles
    WHERE user_id = CAST('11000000-0000-0000-0000-000000000002' AS UUID)
      AND role_id = CAST('10000000-0000-0000-0000-000000000001' AS UUID)
);

INSERT INTO receptionist_assignments (id, user_id, hotel_id, is_active, created_by, updated_by, created_at, updated_at)
SELECT
    CAST('70000000-0000-0000-0000-000000000001' AS UUID),
    CAST('11000000-0000-0000-0000-000000000001' AS UUID),
    CAST('30000000-0000-0000-0000-000000000001' AS UUID),
    TRUE,
    CAST('20000000-0000-0000-0000-000000000001' AS UUID),
    CAST('20000000-0000-0000-0000-000000000001' AS UUID),
    TIMESTAMP WITH TIME ZONE '2026-05-28 00:15:00+00:00',
    TIMESTAMP WITH TIME ZONE '2026-05-28 00:15:00+00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM receptionist_assignments
    WHERE user_id = CAST('11000000-0000-0000-0000-000000000001' AS UUID)
      AND hotel_id = CAST('30000000-0000-0000-0000-000000000001' AS UUID)
);

INSERT INTO receptionist_assignments (id, user_id, hotel_id, is_active, created_by, updated_by, created_at, updated_at)
SELECT
    CAST('70000000-0000-0000-0000-000000000002' AS UUID),
    CAST('11000000-0000-0000-0000-000000000001' AS UUID),
    CAST('30000000-0000-0000-0000-000000000002' AS UUID),
    TRUE,
    CAST('20000000-0000-0000-0000-000000000001' AS UUID),
    CAST('20000000-0000-0000-0000-000000000001' AS UUID),
    TIMESTAMP WITH TIME ZONE '2026-05-28 00:15:00+00:00',
    TIMESTAMP WITH TIME ZONE '2026-05-28 00:15:00+00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM receptionist_assignments
    WHERE user_id = CAST('11000000-0000-0000-0000-000000000001' AS UUID)
      AND hotel_id = CAST('30000000-0000-0000-0000-000000000002' AS UUID)
);

INSERT INTO bookings (
    id,
    user_id,
    guest_id,
    discount_id,
    cancellation_policy_id,
    check_in_date,
    check_out_date,
    actual_check_in_date,
    actual_check_out_date,
    total_guest,
    total_price,
    currency,
    status,
    expired_at,
    note,
    created_by,
    updated_by,
    created_at,
    updated_at
)
SELECT
    CAST('80000000-0000-0000-0000-000000000001' AS UUID),
    CAST('11000000-0000-0000-0000-000000000002' AS UUID),
    NULL,
    NULL,
    NULL,
    DATE '2026-05-29',
    DATE '2026-05-31',
    NULL,
    NULL,
    2,
    1700000.00,
    'VND',
    'CONFIRMED',
    NULL,
    'Seed booking for receptionist check-in and no-show flow.',
    CAST('20000000-0000-0000-0000-000000000002' AS UUID),
    CAST('20000000-0000-0000-0000-000000000002' AS UUID),
    TIMESTAMP WITH TIME ZONE '2026-05-28 01:00:00+00:00',
    TIMESTAMP WITH TIME ZONE '2026-05-28 01:00:00+00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM bookings
    WHERE id = CAST('80000000-0000-0000-0000-000000000001' AS UUID)
);

INSERT INTO bookings (
    id,
    user_id,
    guest_id,
    discount_id,
    cancellation_policy_id,
    check_in_date,
    check_out_date,
    actual_check_in_date,
    actual_check_out_date,
    total_guest,
    total_price,
    currency,
    status,
    expired_at,
    note,
    created_by,
    updated_by,
    created_at,
    updated_at
)
SELECT
    CAST('80000000-0000-0000-0000-000000000002' AS UUID),
    CAST('11000000-0000-0000-0000-000000000002' AS UUID),
    NULL,
    NULL,
    NULL,
    DATE '2026-05-28',
    DATE '2026-05-30',
    TIMESTAMP '2026-05-28 07:30:00',
    NULL,
    3,
    1450000.00,
    'VND',
    'CHECKED_IN',
    NULL,
    'Seed booking for receptionist check-out flow.',
    CAST('11000000-0000-0000-0000-000000000001' AS UUID),
    CAST('11000000-0000-0000-0000-000000000001' AS UUID),
    TIMESTAMP WITH TIME ZONE '2026-05-27 23:00:00+00:00',
    TIMESTAMP WITH TIME ZONE '2026-05-28 00:30:00+00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM bookings
    WHERE id = CAST('80000000-0000-0000-0000-000000000002' AS UUID)
);

INSERT INTO booked_rooms (
    id,
    booking_id,
    room_type_id,
    quantity,
    unit_price,
    room_type_name_snapshot,
    room_type_code_snapshot,
    bed_type_snapshot,
    max_occupancy_snapshot
)
SELECT
    CAST('81000000-0000-0000-0000-000000000001' AS UUID),
    CAST('80000000-0000-0000-0000-000000000001' AS UUID),
    CAST('41000000-0000-0000-0000-000000000001' AS UUID),
    1,
    850000.00,
    'Standard',
    'STD',
    'DOUBLE',
    2
WHERE NOT EXISTS (
    SELECT 1
    FROM booked_rooms
    WHERE id = CAST('81000000-0000-0000-0000-000000000001' AS UUID)
);

INSERT INTO booked_rooms (
    id,
    booking_id,
    room_type_id,
    quantity,
    unit_price,
    room_type_name_snapshot,
    room_type_code_snapshot,
    bed_type_snapshot,
    max_occupancy_snapshot
)
SELECT
    CAST('81000000-0000-0000-0000-000000000002' AS UUID),
    CAST('80000000-0000-0000-0000-000000000002' AS UUID),
    CAST('41000000-0000-0000-0000-000000000005' AS UUID),
    1,
    1450000.00,
    'Deluxe',
    'DLX',
    'DOUBLE',
    3
WHERE NOT EXISTS (
    SELECT 1
    FROM booked_rooms
    WHERE id = CAST('81000000-0000-0000-0000-000000000002' AS UUID)
);

INSERT INTO booking_status_logs (
    id,
    booking_id,
    from_status,
    to_status,
    performed_by,
    performed_by_type,
    note,
    created_at,
    updated_at
)
SELECT
    CAST('82000000-0000-0000-0000-000000000001' AS UUID),
    CAST('80000000-0000-0000-0000-000000000001' AS UUID),
    NULL,
    'CONFIRMED',
    CAST('20000000-0000-0000-0000-000000000002' AS UUID),
    'MANAGER',
    'Seed confirmed booking for front desk workflow.',
    TIMESTAMP WITH TIME ZONE '2026-05-28 01:00:00+00:00',
    TIMESTAMP WITH TIME ZONE '2026-05-28 01:00:00+00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM booking_status_logs
    WHERE id = CAST('82000000-0000-0000-0000-000000000001' AS UUID)
);

INSERT INTO booking_status_logs (
    id,
    booking_id,
    from_status,
    to_status,
    performed_by,
    performed_by_type,
    note,
    created_at,
    updated_at
)
SELECT
    CAST('82000000-0000-0000-0000-000000000002' AS UUID),
    CAST('80000000-0000-0000-0000-000000000002' AS UUID),
    NULL,
    'CONFIRMED',
    CAST('20000000-0000-0000-0000-000000000002' AS UUID),
    'MANAGER',
    'Seed booking created and confirmed.',
    TIMESTAMP WITH TIME ZONE '2026-05-27 23:00:00+00:00',
    TIMESTAMP WITH TIME ZONE '2026-05-27 23:00:00+00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM booking_status_logs
    WHERE id = CAST('82000000-0000-0000-0000-000000000002' AS UUID)
);

INSERT INTO booking_status_logs (
    id,
    booking_id,
    from_status,
    to_status,
    performed_by,
    performed_by_type,
    note,
    created_at,
    updated_at
)
SELECT
    CAST('82000000-0000-0000-0000-000000000003' AS UUID),
    CAST('80000000-0000-0000-0000-000000000002' AS UUID),
    'CONFIRMED',
    'CHECKED_IN',
    CAST('11000000-0000-0000-0000-000000000001' AS UUID),
    'USER',
    'Seed booking already checked in by receptionist.',
    TIMESTAMP WITH TIME ZONE '2026-05-28 00:30:00+00:00',
    TIMESTAMP WITH TIME ZONE '2026-05-28 00:30:00+00:00'
WHERE NOT EXISTS (
    SELECT 1
    FROM booking_status_logs
    WHERE id = CAST('82000000-0000-0000-0000-000000000003' AS UUID)
);
