-- Vendedores (profesionales). El id es el mismo sub del JWT emitido por Auth.
CREATE TABLE professionals (
    id VARCHAR(64) PRIMARY KEY,
    status VARCHAR(16) NOT NULL DEFAULT 'OFFLINE',
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    location_updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_professionals_status CHECK (status IN ('OFFLINE', 'AVAILABLE', 'BUSY'))
);

CREATE INDEX idx_professionals_status_lat ON professionals (status, latitude);
