package com.alamano.core.application.service;

import com.alamano.core.application.port.in.UpdateTrackingUseCase;
import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.application.port.out.TrackingEventPublisherPort;
import com.alamano.core.application.port.out.TrackingRepositoryPort;
import com.alamano.core.domain.professional.GeoPoint;
import com.alamano.core.domain.professional.InvalidSearchAreaException;
import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceStatus;
import com.alamano.core.domain.tracking.EtaCalculator;
import com.alamano.core.domain.tracking.TrackingUpdate;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateTrackingService implements UpdateTrackingUseCase {
    private static final Logger log = LoggerFactory.getLogger(UpdateTrackingService.class);
    private final ServiceRepositoryPort serviceRepository;
    private final TrackingRepositoryPort trackingRepository;
    private final TrackingEventPublisherPort eventPublisher;
    private final EtaCalculator etaCalculator;

    public UpdateTrackingService(ServiceRepositoryPort serviceRepository,
            TrackingRepositoryPort trackingRepository, TrackingEventPublisherPort eventPublisher,
            EtaCalculator etaCalculator) {
        this.serviceRepository = serviceRepository;
        this.trackingRepository = trackingRepository;
        this.eventPublisher = eventPublisher;
        this.etaCalculator = etaCalculator;
    }

    @Override
    public void handleLocation(String professionalId, String serviceId, double latitude, double longitude,
            Instant recordedAt, String correlationId) {
        final UUID id;
        final GeoPoint location;
        try {
            id = UUID.fromString(serviceId);
            location = new GeoPoint(latitude, longitude);
        } catch (IllegalArgumentException | InvalidSearchAreaException exception) {
            log.warn("Se ignora una ubicación con identificador o coordenadas no válidas.");
            return;
        }
        if (recordedAt == null) {
            log.warn("Se ignora una ubicación sin fecha de registro para el servicio {}.", serviceId);
            return;
        }
        Service service = serviceRepository.findById(id).orElse(null);
        if (service == null) {
            log.debug("Se ignora ubicación de un servicio inexistente: {}", serviceId);
            return;
        }
        if (!service.professionalId().equals(professionalId)) {
            log.warn("Se ignora ubicación del vendedor {} para un servicio que pertenece a otro vendedor.", professionalId);
            return;
        }
        if (service.status() == ServiceStatus.COMPLETED || service.status() == ServiceStatus.CANCELLED) return;
        if (!trackingRepository.saveLastLocationIfNewer(serviceId, location, recordedAt)) return;

        GeoPoint destination = service.destinationLatitude() == null || service.destinationLongitude() == null
                ? null : new GeoPoint(service.destinationLatitude(), service.destinationLongitude());
        Integer etaSeconds = etaCalculator.etaSeconds(location, destination, service.status().name());
        eventPublisher.publishTrackingUpdated(
                new TrackingUpdate(serviceId, professionalId, location, etaSeconds, recordedAt), correlationId);
    }
}
