package com.alamano.core.infrastructure.adapter.out.persistence;

import com.alamano.core.application.port.out.ProfessionalQueryPort;
import com.alamano.core.domain.professional.BoundingBox;
import com.alamano.core.domain.professional.GeoPoint;
import com.alamano.core.domain.professional.NearbyProfessional;
import com.alamano.core.domain.professional.SearchArea;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProfessionalJdbcAdapter implements ProfessionalQueryPort {
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
}
