package com.alamano.core.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.application.port.out.TrackingEventPublisherPort;
import com.alamano.core.application.port.out.TrackingRepositoryPort;
import com.alamano.core.domain.professional.GeoPoint;
import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceStatus;
import com.alamano.core.domain.tracking.EtaCalculator;
import com.alamano.core.domain.tracking.TrackingUpdate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateTrackingServiceTest {
    private static final UUID SERVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000337");
    private static final Instant RECORDED_AT = Instant.parse("2026-01-01T12:00:00Z");
    private final MemoryServices services = new MemoryServices();
    private final MemoryTracking tracking = new MemoryTracking();
    private final ListPublisher publisher = new ListPublisher();
    private UpdateTrackingService useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateTrackingService(services, tracking, publisher, new EtaCalculator(25));
        services.service = service(ServiceStatus.EN_ROUTE, "pro-1", 4.69, -74.0628);
    }

    @Test
    void savesAndPublishesEtaForOwningProfessional() {
        useCase.handleLocation("pro-1", SERVICE_ID.toString(), 4.6486, -74.0628, RECORDED_AT, "corr-1");

        assertTrue(tracking.saved);
        assertEquals(1, publisher.updates.size());
        assertTrue(publisher.updates.getFirst().etaSeconds() > 0);
        assertEquals("corr-1", publisher.correlationId);
    }

    @Test
    void ignoresDifferentProfessional() {
        useCase.handleLocation("pro-other", SERVICE_ID.toString(), 4.6486, -74.0628, RECORDED_AT, "corr");
        assertFalse(tracking.saved);
        assertTrue(publisher.updates.isEmpty());
    }

    @Test
    void ignoresCompletedService() {
        services.service = service(ServiceStatus.COMPLETED, "pro-1", 4.69, -74.0628);
        useCase.handleLocation("pro-1", SERVICE_ID.toString(), 4.6486, -74.0628, RECORDED_AT, "corr");
        assertFalse(tracking.saved);
        assertTrue(publisher.updates.isEmpty());
    }

    @Test
    void ignoresMissingServiceWithoutThrowing() {
        services.service = null;
        useCase.handleLocation("pro-1", SERVICE_ID.toString(), 4.6486, -74.0628, RECORDED_AT, "corr");
        assertTrue(publisher.updates.isEmpty());
    }

    @Test
    void ignoresLocationOlderThanPersistedOne() {
        tracking.accept = false;
        useCase.handleLocation("pro-1", SERVICE_ID.toString(), 4.6486, -74.0628, RECORDED_AT, "corr");
        assertFalse(tracking.saved);
        assertTrue(publisher.updates.isEmpty());
    }

    private static Service service(ServiceStatus status, String professionalId, Double latitude, Double longitude) {
        return new Service(SERVICE_ID, professionalId, "client-1", status, 1, RECORDED_AT, RECORDED_AT,
                latitude, longitude);
    }

    private static class MemoryServices implements ServiceRepositoryPort {
        Service service;
        public Service save(Service value) { service = value; return value; }
        public Optional<Service> findById(UUID id) { return Optional.ofNullable(service); }
        public boolean updateStatusIfMatches(UUID id, ServiceStatus expected, long version, Service updated) {
            service = updated; return true;
        }
        public boolean hasActiveService(String professionalId) {
            return service != null && professionalId.equals(service.professionalId()) && !service.isTerminal();
        }
    }

    private static class MemoryTracking implements TrackingRepositoryPort {
        boolean accept = true;
        boolean saved;
        public boolean saveLastLocationIfNewer(String serviceId, GeoPoint location, Instant recordedAt) {
            saved = accept; return accept;
        }
    }

    private static class ListPublisher implements TrackingEventPublisherPort {
        final ArrayList<TrackingUpdate> updates = new ArrayList<>();
        String correlationId;
        public void publishTrackingUpdated(TrackingUpdate update, String correlation) {
            updates.add(update); correlationId = correlation;
        }
    }
}
