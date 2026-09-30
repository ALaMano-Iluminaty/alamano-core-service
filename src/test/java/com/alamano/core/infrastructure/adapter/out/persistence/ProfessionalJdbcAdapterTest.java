package com.alamano.core.infrastructure.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alamano.core.application.port.out.ProfessionalQueryPort;
import com.alamano.core.domain.professional.GeoPoint;
import com.alamano.core.domain.professional.NearbyProfessional;
import com.alamano.core.domain.professional.SearchArea;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProfessionalJdbcAdapterTest {
    private static final double CENTER_LATITUDE = 4.6486;
    private static final double CENTER_LONGITUDE = -74.0628;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ProfessionalQueryPort professionalQuery;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM professionals");
        insert("cerca-1", "AVAILABLE", 4.6576, CENTER_LONGITUDE);
        insert("cerca-2", "AVAILABLE", 4.6756, CENTER_LONGITUDE);
        insert("lejos", "AVAILABLE", 4.7206, CENTER_LONGITUDE);
        insert("ocupado", "BUSY", 4.6531, CENTER_LONGITUDE);
        insert("desconectado", "OFFLINE", 4.6531, CENTER_LONGITUDE);
        jdbc.update(
                "INSERT INTO professionals (id, status, latitude, longitude) VALUES (?, ?, ?, ?)",
                "sin-ubicacion",
                "AVAILABLE",
                null,
                null);
    }

    @Test
    void returnsOnlyNearbyAvailableProfessionalsOrderedByDistance() {
        List<NearbyProfessional> results = professionalQuery.findAvailableWithin(searchArea(5), 50);

        assertEquals(List.of("cerca-1", "cerca-2"), results.stream().map(NearbyProfessional::professionalId).toList());
        assertEquals(1.0, results.get(0).distanceKm(), 0.1);
        assertEquals(3.0, results.get(1).distanceKm(), 0.1);
    }

    @Test
    void widerRadiusIncludesFarProfessionalButExcludesUnavailableAndMissingLocations() {
        List<String> ids = professionalQuery.findAvailableWithin(searchArea(10), 50).stream()
                .map(NearbyProfessional::professionalId)
                .toList();

        assertTrue(ids.containsAll(List.of("cerca-1", "cerca-2", "lejos")));
        assertTrue(!ids.contains("ocupado"));
        assertTrue(!ids.contains("desconectado"));
        assertTrue(!ids.contains("sin-ubicacion"));
    }

    @Test
    void respectsResultLimit() {
        List<NearbyProfessional> results = professionalQuery.findAvailableWithin(searchArea(10), 1);

        assertEquals(1, results.size());
        assertEquals("cerca-1", results.getFirst().professionalId());
    }

    private SearchArea searchArea(double radiusKm) {
        return new SearchArea(new GeoPoint(CENTER_LATITUDE, CENTER_LONGITUDE), radiusKm);
    }

    private void insert(String id, String status, double latitude, double longitude) {
        jdbc.update(
                "INSERT INTO professionals (id, status, latitude, longitude) VALUES (?, ?, ?, ?)",
                id,
                status,
                latitude,
                longitude);
    }
}
