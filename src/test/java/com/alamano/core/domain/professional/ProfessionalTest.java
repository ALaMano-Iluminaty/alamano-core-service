package com.alamano.core.domain.professional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProfessionalTest {
    private static final Instant BEFORE = Instant.parse("2026-09-30T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-09-30T12:00:00Z");
    private static final GeoPoint OLD_LOCATION = new GeoPoint(4.6000, -74.1000);
    private static final GeoPoint NEW_LOCATION = new GeoPoint(4.6486, -74.0628);

    @Test
    void connectFirstTimeCreatesAvailableProfessionalAtVersionZero() {
        Professional professional = Professional.connectFirstTime("pro-1", NEW_LOCATION, NOW);

        assertEquals("pro-1", professional.id());
        assertEquals(ProfessionalStatus.AVAILABLE, professional.status());
        assertEquals(NEW_LOCATION, professional.location());
        assertEquals(NOW, professional.locationUpdatedAt());
        assertEquals(0, professional.version());
    }

    @Test
    void goOnlineFromOfflineBecomesAvailableWithNewLocation() {
        Professional offline = new Professional("pro-1", ProfessionalStatus.OFFLINE, OLD_LOCATION, BEFORE, 3);

        Professional online = offline.goOnline(NEW_LOCATION, NOW);

        assertEquals(ProfessionalStatus.AVAILABLE, online.status());
        assertEquals(NEW_LOCATION, online.location());
        assertEquals(NOW, online.locationUpdatedAt());
        assertEquals(4, online.version());
    }

    @Test
    void goOnlineWhileAvailableUpdatesLocation() {
        Professional available = new Professional("pro-1", ProfessionalStatus.AVAILABLE, OLD_LOCATION, BEFORE, 7);

        Professional online = available.goOnline(NEW_LOCATION, NOW);

        assertEquals(ProfessionalStatus.AVAILABLE, online.status());
        assertEquals(NEW_LOCATION, online.location());
        assertEquals(NOW, online.locationUpdatedAt());
        assertEquals(8, online.version());
    }

    @Test
    void goOnlineWhileBusyIsRejected() {
        Professional busy = new Professional("pro-1", ProfessionalStatus.BUSY, OLD_LOCATION, BEFORE, 2);

        assertThrows(ProfessionalBusyException.class, () -> busy.goOnline(NEW_LOCATION, NOW));
    }
}
