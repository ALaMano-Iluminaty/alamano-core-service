package com.alamano.core.infrastructure.adapter.out.persistence;

import com.alamano.core.application.port.out.ProfessionalQueryPort;
import com.alamano.core.application.port.out.ProfessionalRepositoryPort;
import com.alamano.core.domain.professional.BoundingBox;
import com.alamano.core.domain.professional.GeoPoint;
import com.alamano.core.domain.professional.NearbyProfessional;
import com.alamano.core.domain.professional.Professional;
import com.alamano.core.domain.professional.ProfessionalStatus;
import com.alamano.core.domain.professional.SearchArea;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
/**
 * Acceso a la tabla professionals: la consulta de cercanos (HU2) y la lectura y escritura
 * del agregado Professional (HU4) comparten tabla, por eso viven en el mismo adaptador,
 * igual que ServiceJdbcAdapter con la tabla services.
 */
public class ProfessionalJdbcAdapter implements ProfessionalQueryPort, ProfessionalRepositoryPort {
    private static final String FIND_AVAILABLE_WITHIN = """
            SELECT id, latitude, longitude, distance_km
            FROM (
                SELECT id, latitude, longitude,
                       2 * 6371 * ASIN(SQRT(LEAST(1.0,
                           POWER(SIN(RADIANS(latitude - ?) / 2), 2)
                           + COS(RADIANS(?)) * COS(RADIANS(latitude))
                           * POWER(SIN(RADIANS(longitude - ?) / 2), 2)))) AS distance_km
                FROM professionals
                WHERE status = 'AVAILABLE'
                  AND latitude IS NOT NULL AND longitude IS NOT NULL
                  AND latitude BETWEEN ? AND ?
                  AND longitude BETWEEN ? AND ?
            ) candidates
            WHERE distance_km <= ?
            ORDER BY distance_km
            LIMIT ?
            """;

    private final JdbcTemplate jdbc;

    public ProfessionalJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<NearbyProfessional> findAvailableWithin(SearchArea area, int limit) {
        BoundingBox box = area.boundingBox();
        GeoPoint center = area.center();
        return jdbc.query(
                FIND_AVAILABLE_WITHIN,
                (rs, rowNum) -> new NearbyProfessional(
                        rs.getString("id"),
                        new GeoPoint(rs.getDouble("latitude"), rs.getDouble("longitude")),
                        rs.getDouble("distance_km")),
                center.latitude(),
                center.latitude(),
                center.longitude(),
                box.minLatitude(),
                box.maxLatitude(),
                box.minLongitude(),
                box.maxLongitude(),
                area.radiusKm(),
                limit);
    }

    @Override
    public Optional<Professional> findById(String id) {
        return jdbc.query(
                        """
                        SELECT id, status, latitude, longitude, location_updated_at, version
                        FROM professionals WHERE id = ?
                        """,
                        ProfessionalJdbcAdapter::mapProfessional,
                        id)
                .stream()
                .findFirst();
    }

    @Override
    public boolean insert(Professional professional) {
        GeoPoint location = professional.location();
        try {
            jdbc.update(
                    """
                    INSERT INTO professionals (id, status, latitude, longitude, location_updated_at, version)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    professional.id(),
                    professional.status().name(),
                    location == null ? null : location.latitude(),
                    location == null ? null : location.longitude(),
                    toTimestamp(professional.locationUpdatedAt()),
                    professional.version());
            return true;
        } catch (DuplicateKeyException e) {
            // Otro request creó al mismo vendedor al mismo tiempo.
            return false;
        }
    }

    @Override
    public boolean updateIfVersionMatches(Professional updated, long expectedVersion) {
        GeoPoint location = updated.location();
        int rows = jdbc.update(
                """
                UPDATE professionals
                SET status = ?, latitude = ?, longitude = ?, location_updated_at = ?, version = ?
                WHERE id = ? AND version = ?
                """,
                updated.status().name(),
                location == null ? null : location.latitude(),
                location == null ? null : location.longitude(),
                toTimestamp(updated.locationUpdatedAt()),
                updated.version(),
                updated.id(),
                expectedVersion);
        return rows == 1;
    }

    @Override
    public boolean markBusyIfAvailable(String professionalId) {
        int rows = jdbc.update(
                """
                UPDATE professionals
                SET status = 'BUSY', version = version + 1
                WHERE id = ? AND status = 'AVAILABLE'
                """,
                professionalId);
        return rows == 1;
    }

    @Override
    public boolean releaseIfBusy(String professionalId) {
        int rows = jdbc.update(
                """
                UPDATE professionals
                SET status = 'AVAILABLE', version = version + 1
                WHERE id = ? AND status = 'BUSY'
                """,
                professionalId);
        return rows == 1;
    }

    private static Professional mapProfessional(ResultSet rs, int rowNum) throws SQLException {
        double latitude = rs.getDouble("latitude");
        boolean latitudeNull = rs.wasNull();
        double longitude = rs.getDouble("longitude");
        boolean longitudeNull = rs.wasNull();
        GeoPoint location = latitudeNull || longitudeNull ? null : new GeoPoint(latitude, longitude);
        Timestamp locationUpdatedAt = rs.getTimestamp("location_updated_at");
        return new Professional(
                rs.getString("id"),
                ProfessionalStatus.valueOf(rs.getString("status")),
                location,
                locationUpdatedAt == null ? null : locationUpdatedAt.toInstant(),
                rs.getLong("version"));
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
