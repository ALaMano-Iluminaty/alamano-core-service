package com.alamano.core.infrastructure.adapter.out.persistence;

import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class ServiceJdbcAdapter implements ServiceRepositoryPort {
    private static final RowMapper<Service> ROW_MAPPER = ServiceJdbcAdapter::mapRow;
    private final JdbcTemplate jdbc;

    public ServiceJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Service save(Service service) {
        jdbc.update(
                """
                INSERT INTO services (id, professional_id, client_id, status, version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                service.id(),
                service.professionalId(),
                service.clientId(),
                service.status().name(),
                service.version(),
                Timestamp.from(service.createdAt()),
                Timestamp.from(service.updatedAt()));
        return service;
    }

    @Override
    public Optional<Service> findById(UUID serviceId) {
        return jdbc.query(
                        """
                        SELECT id, professional_id, client_id, status, version, created_at, updated_at
                        FROM services WHERE id = ?
                        """,
                        ROW_MAPPER,
                        serviceId)
                .stream()
                .findFirst();
    }

    @Override
    public boolean updateStatusIfMatches(
            UUID serviceId, ServiceStatus expectedStatus, long expectedVersion, Service updated) {
        int rows = jdbc.update(
                """
                UPDATE services
                SET status = ?, version = ?, updated_at = ?
                WHERE id = ? AND status = ? AND version = ?
                """,
                updated.status().name(),
                updated.version(),
                Timestamp.from(updated.updatedAt()),
                serviceId,
                expectedStatus.name(),
                expectedVersion);
        return rows == 1;
    }

    private static Service mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Service(
                rs.getObject("id", UUID.class),
                rs.getString("professional_id"),
                rs.getString("client_id"),
                ServiceStatus.valueOf(rs.getString("status")),
                rs.getLong("version"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
