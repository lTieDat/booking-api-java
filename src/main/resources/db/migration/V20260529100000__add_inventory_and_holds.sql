CREATE TABLE room_inventories (
    id              UUID PRIMARY KEY,
    room_type_id    UUID NOT NULL,
    date            DATE NOT NULL,
    total_units     INT  NOT NULL,
    booked_units    INT  NOT NULL DEFAULT 0,
    held_units      INT  NOT NULL DEFAULT 0,
    available_units INT  NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_inventories_room_type FOREIGN KEY (room_type_id) REFERENCES room_types (id),
    CONSTRAINT uk_room_inventories_room_type_date UNIQUE (room_type_id, date),
    CONSTRAINT chk_room_inventories_units_non_negative
        CHECK (total_units >= 0 AND booked_units >= 0 AND held_units >= 0 AND available_units >= 0),
    CONSTRAINT chk_room_inventories_available_units
        CHECK (available_units = total_units - booked_units - held_units)
);

CREATE TABLE inventory_holds (
    id           UUID PRIMARY KEY,
    booking_id   UUID NOT NULL,
    payment_id   UUID,
    room_type_id UUID NOT NULL,
    date         DATE NOT NULL,
    quantity     INT NOT NULL,
    status       VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    expires_at   TIMESTAMP NOT NULL,
    released_at  TIMESTAMP,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_holds_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_inventory_holds_room_type FOREIGN KEY (room_type_id) REFERENCES room_types (id),
    CONSTRAINT uk_inventory_holds_booking_room_type_date UNIQUE (booking_id, room_type_id, date),
    CONSTRAINT chk_inventory_holds_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_inventory_holds_status
        CHECK (status IN ('ACTIVE', 'RELEASED', 'EXPIRED', 'CONSUMED'))
);

CREATE INDEX idx_inventory_holds_room_type_date_status ON inventory_holds (room_type_id, date, status);
CREATE INDEX idx_inventory_holds_status_expires_at ON inventory_holds (status, expires_at);
