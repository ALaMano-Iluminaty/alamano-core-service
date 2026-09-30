package com.alamano.core.infrastructure.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alamano.core.application.port.out.ProfessionalRepositoryPort;
import com.alamano.core.domain.professional.GeoPoint;
import com.alamano.core.domain.professional.Professional;
import com.alamano.core.domain.professional.ProfessionalStatus;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProfessionalRepositoryJdbcTest {
    private static final Instant CONNECTED_AT = Instant.parse("2026-09-30T12:00:00Z");
    private static final GeoPoint LOCATION = new GeoPoint(4.6486, -74.0628);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ProfessionalRepositoryPort repository;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM professionals");
    }

    @Test
    void insertReturnsFalseForDuplicateId() {
        Professional professional = Professional.connectFirstTime("pro-1", LOCATION, CONNECTED_AT);

        assertTrue(repository.insert(professional));
        assertFalse(repository.insert(professional));
    }

    @Test
    void findByIdReturnsStoredDataAndNullLocationWhenMissing() {
        repository.insert(Professional.connectFirstTime("pro-1", LOCATION, CONNECTED_AT));
        repository.insert(new Professional("pro-2", ProfessionalStatus.OFFLINE, null, null, 0));

        Professional withLocation = repository.findById("pro-1").orElseThrow();
        assertEquals(ProfessionalStatus.AVAILABLE, withLocation.status());
        assertEquals(LOCATION, withLocation.location());
        assertEquals(CONNECTED_AT, withLocation.locationUpdatedAt());
        assertEquals(0, withLocation.version());

        Professional withoutLocation = repository.findById("pro-2").orElseThrow();
        assertEquals(ProfessionalStatus.OFFLINE, withoutLocation.status());
        assertNull(withoutLocation.location());
        assertNull(withoutLocation.locationUpdatedAt());

        assertTrue(repository.findById("no-existe").isEmpty());
    }

    @Test
    void updateAppliesOnlyWhenVersionMatches() {
        Professional offline = new Professional("pro-1", ProfessionalStatus.OFFLINE, null, null, 0);
        repository.insert(offline);
        Professional online = offline.goOnline(LOCATION, CONNECTED_AT);

        assertTrue(repository.updateIfVersionMatches(online, 0));
        assertEquals(online, repository.findById("pro-1").orElseThrow());

        Professional stale = online.goOnline(new GeoPoint(4.7, -74.0), CONNECTED_AT.plusSeconds(60));
        assertFalse(repository.updateIfVersionMatches(stale, 0));
        assertEquals(online, repository.findById("pro-1").orElseThrow());
    }
}
