package com.alamano.core.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alamano.core.application.port.out.ProfessionalEventPublisherPort;
import com.alamano.core.application.port.out.ProfessionalRepositoryPort;
import com.alamano.core.domain.professional.GeoPoint;
import com.alamano.core.domain.professional.Professional;
import com.alamano.core.domain.professional.DisconnectReason;
import com.alamano.core.domain.professional.ProfessionalBusyException;
import com.alamano.core.domain.professional.ProfessionalConcurrentUpdateException;
import com.alamano.core.domain.professional.ProfessionalStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConnectProfessionalServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-30T12:00:00Z");
    private static final double LATITUDE = 4.6486;
    private static final double LONGITUDE = -74.0628;

    private InMemoryProfessionalRepository repository;
    private RecordingPublisher publisher;
    private ConnectProfessionalService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProfessionalRepository();
        publisher = new RecordingPublisher();
        service = new ConnectProfessionalService(repository, publisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void firstConnectionInsertsAndPublishesOnce() {
        Professional result = service.connect("pro-1", LATITUDE, LONGITUDE, "corr-1");

        assertEquals(ProfessionalStatus.AVAILABLE, result.status());
        assertEquals(0, result.version());
        assertEquals(result, repository.stored.get("pro-1"));
        assertEquals(1, publisher.published.size());
        Published event = publisher.published.getFirst();
        assertEquals("pro-1", event.professional().id());
        assertEquals(new GeoPoint(LATITUDE, LONGITUDE), event.professional().location());
        assertEquals("corr-1", event.correlationId());
    }

    @Test
    void offlineProfessionalIsUpdatedAndPublished() {
        repository.stored.put("pro-1", new Professional("pro-1", ProfessionalStatus.OFFLINE, null, null, 4));

        Professional result = service.connect("pro-1", LATITUDE, LONGITUDE, "corr-1");

        assertEquals(ProfessionalStatus.AVAILABLE, result.status());
        assertEquals(5, result.version());
        assertEquals(new GeoPoint(LATITUDE, LONGITUDE), repository.stored.get("pro-1").location());
        assertEquals(List.of(new Published(result, "corr-1")), publisher.published);
    }

    @Test
    void busyProfessionalIsRejectedWithoutPublishing() {
        Professional busy = new Professional("pro-1", ProfessionalStatus.BUSY, new GeoPoint(4.6, -74.1), NOW, 2);
        repository.stored.put("pro-1", busy);

        assertThrows(ProfessionalBusyException.class, () -> service.connect("pro-1", LATITUDE, LONGITUDE, "corr-1"));

        assertEquals(busy, repository.stored.get("pro-1"));
        assertTrue(publisher.published.isEmpty());
    }

    @Test
    void retriesOnceWhenVersionChangedAndPublishesOnlyOnce() {
        repository.stored.put("pro-1", new Professional("pro-1", ProfessionalStatus.OFFLINE, null, null, 0));
        repository.failingUpdates = 1;

        Professional result = service.connect("pro-1", LATITUDE, LONGITUDE, "corr-1");

        assertEquals(2, repository.updateAttempts);
        assertEquals(ProfessionalStatus.AVAILABLE, repository.stored.get("pro-1").status());
        assertEquals(List.of(new Published(result, "corr-1")), publisher.published);
    }

    @Test
    void failsAfterTwoConflictingUpdatesWithoutPublishing() {
        Professional offline = new Professional("pro-1", ProfessionalStatus.OFFLINE, null, null, 0);
        repository.stored.put("pro-1", offline);
        repository.failingUpdates = 2;

        assertThrows(
                ProfessionalConcurrentUpdateException.class,
                () -> service.connect("pro-1", LATITUDE, LONGITUDE, "corr-1"));

        assertEquals(2, repository.updateAttempts);
        assertEquals(offline, repository.stored.get("pro-1"));
        assertTrue(publisher.published.isEmpty());
    }

    @Test
    void generatesCorrelationIdWhenMissing() {
        service.connect("pro-1", LATITUDE, LONGITUDE, null);
        service.connect("pro-1", LATITUDE, LONGITUDE, " ");

        assertEquals(2, publisher.published.size());
        for (Published event : publisher.published) {
            assertNotNull(event.correlationId());
            assertFalse(event.correlationId().isBlank());
        }
    }

    private record Published(Professional professional, String correlationId) {}

    private static final class RecordingPublisher implements ProfessionalEventPublisherPort {
        private final List<Published> published = new ArrayList<>();

        @Override
        public void publishOnline(Professional professional, String correlationId) {
            published.add(new Published(professional, correlationId));
        }

        @Override
        public void publishDisconnected(Professional professional, DisconnectReason reason, String correlationId) {}
    }

    /** Repositorio en memoria; failingUpdates simula que otro request cambió la versión. */
    private static final class InMemoryProfessionalRepository implements ProfessionalRepositoryPort {
        private final Map<String, Professional> stored = new HashMap<>();
        private int failingUpdates;
        private int updateAttempts;

        @Override
        public Optional<Professional> findById(String id) {
            return Optional.ofNullable(stored.get(id));
        }

        @Override
        public boolean insert(Professional professional) {
            return stored.putIfAbsent(professional.id(), professional) == null;
        }

        @Override
        public boolean updateIfVersionMatches(Professional updated, long expectedVersion) {
            updateAttempts++;
            if (failingUpdates > 0) {
                failingUpdates--;
                return false;
            }
            Professional current = stored.get(updated.id());
            if (current == null || current.version() != expectedVersion) {
                return false;
            }
            stored.put(updated.id(), updated);
            return true;
        }
    }
}
