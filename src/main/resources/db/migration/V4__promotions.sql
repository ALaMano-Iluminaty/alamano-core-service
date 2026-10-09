-- Promociones con cupos limitados (HU12). El contador atómico vive en Redis: promotion:{id}:cupos.
CREATE TABLE promotions (
    id UUID PRIMARY KEY,
    professional_id VARCHAR(64) NOT NULL,
    description VARCHAR(500) NOT NULL,
    total_slots INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_promotions_slots CHECK (total_slots > 0)
);

CREATE INDEX idx_promotions_professional ON promotions (professional_id);
