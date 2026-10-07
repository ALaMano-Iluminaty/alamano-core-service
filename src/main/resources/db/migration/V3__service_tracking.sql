ALTER TABLE services ADD COLUMN destination_latitude DOUBLE PRECISION;
ALTER TABLE services ADD COLUMN destination_longitude DOUBLE PRECISION;
ALTER TABLE services ADD COLUMN last_latitude DOUBLE PRECISION;
ALTER TABLE services ADD COLUMN last_longitude DOUBLE PRECISION;
ALTER TABLE services ADD COLUMN last_tracked_at TIMESTAMP;
