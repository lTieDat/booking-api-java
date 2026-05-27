ALTER TABLE bookings
    DROP CONSTRAINT IF EXISTS fk_bookings_room_type;

ALTER TABLE bookings
    DROP CONSTRAINT IF EXISTS fk_bookings_assigned_room;

ALTER TABLE bookings
    DROP COLUMN IF EXISTS room_type_id;

ALTER TABLE bookings
    DROP COLUMN IF EXISTS assigned_room_id;

CREATE TABLE amenities (
    id          UUID PRIMARY KEY,
    room_type_id UUID NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(300),
    quantity    INT          NOT NULL DEFAULT 1,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by  UUID,
    updated_by  UUID,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_amenities_room_type FOREIGN KEY (room_type_id) REFERENCES room_types (id),
    CONSTRAINT uk_amenities_room_type_code UNIQUE (room_type_id, code)
);

CREATE TABLE booked_rooms (
    id                       UUID PRIMARY KEY,
    booking_id               UUID           NOT NULL,
    room_type_id             UUID           NOT NULL,
    quantity                 INT            NOT NULL,
    unit_price               NUMERIC(10, 2) NOT NULL,
    room_type_name_snapshot  VARCHAR(50),
    room_type_code_snapshot  VARCHAR(30),
    bed_type_snapshot        VARCHAR(50),
    max_occupancy_snapshot   INT,
    CONSTRAINT fk_booked_rooms_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_booked_rooms_room_type FOREIGN KEY (room_type_id) REFERENCES room_types (id)
);

CREATE INDEX idx_amenities_room_type_id ON amenities (room_type_id);
CREATE INDEX idx_booked_rooms_booking_id ON booked_rooms (booking_id);
CREATE INDEX idx_booked_rooms_room_type_id ON booked_rooms (room_type_id);
