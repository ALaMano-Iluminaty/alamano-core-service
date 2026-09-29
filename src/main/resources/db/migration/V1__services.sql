CREATE TABLE services (
    id UUID PRIMARY KEY,
    professional_id VARCHAR(64) NOT NULL,
    client_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_services_professional ON services (professional_id);
