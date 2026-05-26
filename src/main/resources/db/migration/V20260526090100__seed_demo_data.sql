INSERT INTO app_users (
    id,
    email,
    full_name,
    password_hash,
    created_at
) VALUES (
    '7d02d18e-cff1-4a39-a91b-54e711e15423',
    'demo@booking.local',
    'Demo User',
    '$2y$10$lRPKgqrnxfLzvS6OW3TLguK4m0Yy816JDp6UFq9H6yGilMe/72D0O',
    TIMESTAMP WITH TIME ZONE '2026-04-23 00:00:00+00:00'
);

INSERT INTO info_messages (
    id,
    message,
    created_at
) VALUES
(
    'f7f95d9f-34ec-4862-8ec7-9da14afe23fd',
    'Flyway baseline schema created successfully.',
    TIMESTAMP WITH TIME ZONE '2026-04-23 00:05:00+00:00'
),
(
    '1ea16708-325b-4b44-93b2-770e6a9a742f',
    'Demo data seeded by Flyway.',
    TIMESTAMP WITH TIME ZONE '2026-04-23 00:10:00+00:00'
);
