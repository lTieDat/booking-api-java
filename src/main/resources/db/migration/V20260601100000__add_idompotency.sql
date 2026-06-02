ALTER TABLE bookings
ADD COLUMN client_request_id VARCHAR(120);

ALTER TABLE bookings
ADD COLUMN request_hash VARCHAR(128);

CREATE UNIQUE INDEX uk_bookings_user_client_request ON bookings (user_id, client_request_id);
