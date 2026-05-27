CREATE TABLE roles (
    id   UUID PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE users (
    id         UUID PRIMARY KEY,
    name       VARCHAR(40)  NOT NULL,
    username   VARCHAR(15)  NOT NULL,
    email      VARCHAR(40)  NOT NULL,
    password   VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email)
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE discounts (
    id            UUID PRIMARY KEY,
    code          VARCHAR(20)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    discount_type VARCHAR(30)  NOT NULL DEFAULT 'FIXED_AMOUNT',
    min_order_value INT        NOT NULL DEFAULT 0,
    max_order_value INT        NOT NULL DEFAULT 0,
    start_date      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_date        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active       BOOLEAN    NOT NULL DEFAULT FALSE,
    max_usage       INT,
    used_count      INT,
    created_by    UUID,
    updated_by    UUID,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_discounts_code UNIQUE (code)
);

CREATE TABLE hotels (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    address     VARCHAR(200),
    city        VARCHAR(100),
    country     VARCHAR(100),
    created_by  UUID,
    updated_by  UUID,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE cancellation_policies (
    id          UUID PRIMARY KEY,
    hotel_id    UUID,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_active   BOOLEAN NOT NULL DEFAULT FALSE,
    created_by  UUID,
    updated_by  UUID,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_cancellation_policies_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id)
);

CREATE TABLE room_types (
    id            UUID PRIMARY KEY,
    hotel_id      UUID           NOT NULL,
    name          VARCHAR(50)    NOT NULL,
    code          VARCHAR(30)    NOT NULL,
    max_adults    INT            NOT NULL,
    max_children  INT            NOT NULL DEFAULT 0,
    max_occupancy INT            NOT NULL,
    bed_type      VARCHAR(50),
    description   VARCHAR(500),
    base_price    NUMERIC(10, 2),
    is_active     BOOLEAN        NOT NULL DEFAULT TRUE,
    created_by    UUID,
    updated_by    UUID,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_room_types_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT uk_room_types_hotel_code UNIQUE (hotel_id, code)
);

CREATE TABLE rooms (
    id           UUID PRIMARY KEY,
    room_type_id UUID           NOT NULL,
    room_number  VARCHAR(20)    NOT NULL,
    floor        INT,
    status       VARCHAR(30),
    is_active    BOOLEAN        NOT NULL DEFAULT TRUE,
    created_by   UUID,
    updated_by   UUID,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_rooms_room_type FOREIGN KEY (room_type_id) REFERENCES room_types (id),
    CONSTRAINT uk_rooms_room_type_number UNIQUE (room_type_id, room_number)
);

CREATE TABLE bookings (
    id             UUID PRIMARY KEY,
    user_id        UUID           NOT NULL,
    room_type_id   UUID           NOT NULL,
    assigned_room_id UUID,
    guest_id       UUID,
    discount_id    UUID,
    cancellation_policy_id UUID,
    check_in_date  DATE           NOT NULL,
    check_out_date DATE           NOT NULL,
    actual_check_in_date TIMESTAMP,
    actual_check_out_date TIMESTAMP,
    total_guest    INT,
    total_price    NUMERIC(10, 2),
    currency       VARCHAR(3)     NOT NULL DEFAULT 'VND',
    status         VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    expired_at     TIMESTAMP,
    note           VARCHAR(300),
    created_by     UUID,
    updated_by     UUID,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_bookings_room_type FOREIGN KEY (room_type_id) REFERENCES room_types (id),
    CONSTRAINT fk_bookings_assigned_room FOREIGN KEY (assigned_room_id) REFERENCES rooms (id),
    CONSTRAINT fk_bookings_discount FOREIGN KEY (discount_id) REFERENCES discounts (id),
    CONSTRAINT fk_bookings_cancellation_policy FOREIGN KEY (cancellation_policy_id) REFERENCES cancellation_policies (id)
);

INSERT INTO roles (id, name) VALUES
    ('10000000-0000-0000-0000-000000000001', 'ROLE_USER'),
    ('10000000-0000-0000-0000-000000000002', 'ROLE_ADMIN');
