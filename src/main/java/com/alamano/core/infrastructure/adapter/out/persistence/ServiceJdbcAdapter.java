package com.alamano.core.infrastructure.adapter.out.persistence;

import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.application.port.out.TrackingRepositoryPort;
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
public class ServiceJdbcAdapter implements ServiceRepositoryPort, TrackingRepositoryPort {
    private static final RowMapper<Service> ROW_MAPPER = ServiceJdbcAdapter::mapRow;
    private final JdbcTemplate jdbc;

    public ServiceJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Service save(Service service) {
        jdbc.update(
                """
                INSERT INTO services (id, professional_id, client_id, status, version, created_at, updated_at,
                    destination_latitude, destination_longitude)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                service.id(),
                service.professionalId(),
                service.clientId(),
                service.status().name(),
                service.version(),
                Timestamp.from(service.createdAt()),
                Timestamp.from(service.updatedAt()),
                service.destinationLatitude(),
                service.destinationLongitude());
        return service;
    }

    @Override
    public Optional<Service> findById(UUID serviceId) {
        return jdbc.query(
                        """
                        SELECT id, professional_id, client_id, status, version, created_at, updated_at,
                            destination_latitude, destination_longitude,
                            last_latitude, last_longitude, last_tracked_at
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

    @Override
    public boolean hasActiveService(String professionalId) {
        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM services
                WHERE professional_id = ? AND status NOT IN ('COMPLETED', 'CANCELLED')
                """,
                Integer.class,
                professionalId);
        return count != null && count > 0;
    }

    @Override
    public boolean saveLastLocationIfNewer(String serviceId, com.alamano.core.domain.professional.GeoPoint location,
            Instant recordedAt) {
        int rows = jdbc.update(
                """
                UPDATE services
                SET last_latitude = ?, last_longitude = ?, last_tracked_at = ?
                WHERE id = ? AND (last_tracked_at IS NULL OR last_tracked_at < ?)
                """,
                location.latitude(), location.longitude(), Timestamp.from(recordedAt), UUID.fromString(serviceId),
                Timestamp.from(recordedAt));
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
                rs.getTimestamp("updated_at").toInstant(),
                (Double) rs.getObject("destination_latitude"),
                (Double) rs.getObject("destination_longitude"),
                (Double) rs.getObject("last_latitude"),
                (Double) rs.getObject("last_longitude"),
                rs.getTimestamp("last_tracked_at") == null ? null : rs.getTimestamp("last_tracked_at").toInstant());
    }
}
