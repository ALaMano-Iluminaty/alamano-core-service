package com.alamano.core.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.alamano.core.application.port.out.ProfessionalEventPublisherPort;
import com.alamano.core.application.port.out.ProfessionalRepositoryPort;
import com.alamano.core.domain.professional.DisconnectReason;
import com.alamano.core.domain.professional.GeoPoint;
import com.alamano.core.domain.professional.Professional;
import com.alamano.core.domain.professional.ProfessionalBusyException;
import com.alamano.core.domain.professional.ProfessionalConcurrentUpdateException;
import com.alamano.core.domain.professional.ProfessionalNotFoundException;
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

class DisconnectProfessionalServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-30T12:00:00Z");
    private static final Instant LAST_ONLINE = NOW.minusSeconds(120);
    private static final GeoPoint LOCATION = new GeoPoint(4.6486, -74.0628);

    private final MemoryRepository repository = new MemoryRepository();
    private final RecordingPublisher publisher = new RecordingPublisher();
    private DisconnectProfessionalService service;

    @BeforeEach
    void setUp() {
        repository.stored.clear();
        repository.failingUpdates = 0;
        repository.updateAttempts = 0;
        publisher.published.clear();
        service = new DisconnectProfessionalService(repository, publisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void manualDisconnectSavesOfflineAndPublishesOnce() {
        Professional available = professional(ProfessionalStatus.AVAILABLE, 2, LAST_ONLINE);
        repository.stored.put(available.id(), available);
        Professional result = service.goOffline("pro-1", "corr-1");
        assertEquals(ProfessionalStatus.OFFLINE, result.status());
        assertEquals(result, repository.stored.get("pro-1"));
        assertEquals(List.of(new Published(result, DisconnectReason.MANUAL, "corr-1")), publisher.published);
    }

    @Test
    void manualDisconnectOfOfflineProfessionalIsIdempotent() {
        Professional offline = professional(ProfessionalStatus.OFFLINE, 3, LAST_ONLINE);
        repository.stored.put(offline.id(), offline);
        assertEquals(offline, service.goOffline("pro-1", "corr-1"));
        assertEquals(0, repository.updateAttempts);
        assertTrue(publisher.published.isEmpty());
    }

    @Test
    void busyProfessionalCannotDisconnectManually() {
        Professional busy = professional(ProfessionalStatus.BUSY, 3, LAST_ONLINE);
        repository.stored.put(busy.id(), busy);
        assertThrows(ProfessionalBusyException.class, () -> service.goOffline("pro-1", "corr-1"));
        assertTrue(publisher.published.isEmpty());
    }

    @Test
    void manualDisconnectOfUnknownProfessionalFails() {
        assertThrows(ProfessionalNotFoundException.class, () -> service.goOffline("missing", "corr-1"));
    }

    @Test
    void manualDisconnectRetriesConflictAndPublishesOnce() {
        repository.stored.put("pro-1", professional(ProfessionalStatus.AVAILABLE, 3, LAST_ONLINE));
        repository.failingUpdates = 1;
        service.goOffline("pro-1", "corr-1");
        assertEquals(2, repository.updateAttempts);
        assertEquals(1, publisher.published.size());
        assertEquals(DisconnectReason.MANUAL, publisher.published.getFirst().reason());
    }

    @Test
    void lostConnectionDisconnectsAndPublishesWhenNoNewerConnectionExists() {
        repository.stored.put("pro-1", professional(ProfessionalStatus.AVAILABLE, 3, LAST_ONLINE));
        service.handleConnectionLost("pro-1", NOW.minusSeconds(60), "corr-lost");
        assertEquals(ProfessionalStatus.OFFLINE, repository.stored.get("pro-1").status());
        assertEquals(DisconnectReason.CONNECTION_LOST, publisher.published.getFirst().reason());
    }

    @Test
    void lostConnectionAfterReconnectIsIgnored() {
        Professional reconnected = professional(ProfessionalStatus.AVAILABLE, 4, NOW.minusSeconds(30));
        repository.stored.put("pro-1", reconnected);
        service.handleConnectionLost("pro-1", NOW.minusSeconds(60), "corr-lost");
        assertEquals(reconnected, repository.stored.get("pro-1"));
        assertTrue(publisher.published.isEmpty());
    }

    @Test
    void lostConnectionForBusyProfessionalIsIgnored() {
        Professional busy = professional(ProfessionalStatus.BUSY, 4, LAST_ONLINE);
        repository.stored.put("pro-1", busy);
        assertDoesNotThrow(() -> service.handleConnectionLost("pro-1", NOW.minusSeconds(60), "corr-lost"));
        assertEquals(busy, repository.stored.get("pro-1"));
        assertTrue(publisher.published.isEmpty());
    }

    @Test
    void lostConnectionForUnknownProfessionalIsIgnored() {
        assertDoesNotThrow(() -> service.handleConnectionLost("missing", NOW, "corr-lost"));
        assertTrue(publisher.published.isEmpty());
    }

    @Test
    void lostConnectionWithTwoVersionConflictsDoesNotThrowOrPublish() {
        Professional available = professional(ProfessionalStatus.AVAILABLE, 4, LAST_ONLINE);
        repository.stored.put("pro-1", available);
        repository.failingUpdates = 2;
        assertDoesNotThrow(() -> service.handleConnectionLost("pro-1", NOW.minusSeconds(60), "corr-lost"));
        assertEquals(available, repository.stored.get("pro-1"));
        assertEquals(2, repository.updateAttempts);
        assertTrue(publisher.published.isEmpty());
    }

    private Professional professional(ProfessionalStatus status, long version, Instant updatedAt) {
        return new Professional("pro-1", status, LOCATION, updatedAt, version);
    }

    private record Published(Professional professional, DisconnectReason reason, String correlationId) {}

    private static final class RecordingPublisher implements ProfessionalEventPublisherPort {
        private final List<Published> published = new ArrayList<>();
        @Override public void publishOnline(Professional professional, String correlationId) {}
        @Override public void publishDisconnected(Professional professional, DisconnectReason reason, String correlationId) {
            published.add(new Published(professional, reason, correlationId));
        }
    }

    private static final class MemoryRepository implements ProfessionalRepositoryPort {
        private final Map<String, Professional> stored = new HashMap<>();
        private int failingUpdates;
        private int updateAttempts;
        @Override public Optional<Professional> findById(String id) { return Optional.ofNullable(stored.get(id)); }
        @Override public boolean insert(Professional professional) { return stored.putIfAbsent(professional.id(), professional) == null; }
        @Override public boolean updateIfVersionMatches(Professional updated, long expectedVersion) {
            updateAttempts++;
            if (failingUpdates > 0) { failingUpdates--; return false; }
            Professional current = stored.get(updated.id());
            if (current == null || current.version() != expectedVersion) return false;
            stored.put(updated.id(), updated);
            return true;
        }
    }
}
