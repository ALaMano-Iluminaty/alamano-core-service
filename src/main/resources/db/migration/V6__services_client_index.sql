-- Servicios activos del cliente (GET /api/services/active). El del profesional ya existe en V1.
CREATE INDEX idx_services_client ON services (client_id);
