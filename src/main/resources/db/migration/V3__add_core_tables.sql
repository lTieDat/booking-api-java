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

CREATE TABLE rooms (
    id              UUID PRIMARY KEY,
    hotel_id        UUID           NOT NULL,
    room_number     VARCHAR(20)    NOT NULL,
    room_type       VARCHAR(50),
    capacity        INT,
    price_per_night NUMERIC(10, 2),
    created_by      UUID,
    updated_by      UUID,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_rooms_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id)
);

CREATE TABLE bookings (
    id             UUID PRIMARY KEY,
    user_id        UUID           NOT NULL,
    room_id        UUID           NOT NULL,
    check_in_date  DATE           NOT NULL,
    check_out_date DATE           NOT NULL,
    total_price    NUMERIC(10, 2),
    status         VARCHAR(30)    NOT NULL DEFAULT 'CONFIRMED',
    created_by     UUID,
    updated_by     UUID,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_bookings_room FOREIGN KEY (room_id) REFERENCES rooms (id)
);

INSERT INTO roles (id, name) VALUES
    ('10000000-0000-0000-0000-000000000001', 'ROLE_USER'),
    ('10000000-0000-0000-0000-000000000002', 'ROLE_ADMIN');
