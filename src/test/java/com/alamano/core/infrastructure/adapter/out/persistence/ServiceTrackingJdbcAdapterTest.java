package com.alamano.core.infrastructure.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.application.port.out.TrackingRepositoryPort;
import com.alamano.core.domain.professional.GeoPoint;
import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ServiceTrackingJdbcAdapterTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ServiceRepositoryPort services;
    @Autowired TrackingRepositoryPort tracking;

    @BeforeEach
    void clearServices() { jdbc.update("DELETE FROM services"); }

    @Test
    void savesNewerLocationAndRejectsOlderLocation() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        services.save(Service.createReserved(id, "pro-1", "client-1", createdAt));
        Instant newer = createdAt.plusSeconds(20);
        Instant older = createdAt.plusSeconds(10);

        assertTrue(tracking.saveLastLocationIfNewer(id.toString(), new GeoPoint(4.65, -74.06), newer));
        assertFalse(tracking.saveLastLocationIfNewer(id.toString(), new GeoPoint(4.66, -74.07), older));
        assertEquals(4.65, jdbc.queryForObject("SELECT last_latitude FROM services WHERE id = ?", Double.class, id));
        assertEquals(newer, jdbc.queryForObject("SELECT last_tracked_at FROM services WHERE id = ?",
                java.sql.Timestamp.class, id).toInstant());
    }

    @Test
    void persistsOptionalDestinationAndLeavesItNullWhenOmitted() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        UUID withDestination = UUID.randomUUID();
        UUID withoutDestination = UUID.randomUUID();
        services.save(Service.createReserved(withDestination, "pro-1", "client-1", now, 4.65, -74.06));
        services.save(Service.createReserved(withoutDestination, "pro-2", "client-2", now));

        Service saved = services.findById(withDestination).orElseThrow();
        Service absent = services.findById(withoutDestination).orElseThrow();
        assertEquals(4.65, saved.destinationLatitude());
        assertEquals(-74.06, saved.destinationLongitude());
        assertNull(absent.destinationLatitude());
        assertNull(absent.destinationLongitude());
    }
}
