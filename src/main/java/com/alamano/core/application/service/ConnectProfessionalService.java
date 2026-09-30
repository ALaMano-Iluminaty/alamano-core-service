package com.alamano.core.application.service;

import com.alamano.core.application.port.in.ConnectProfessionalUseCase;
import com.alamano.core.application.port.out.ProfessionalEventPublisherPort;
import com.alamano.core.application.port.out.ProfessionalRepositoryPort;
import com.alamano.core.domain.professional.GeoPoint;
import com.alamano.core.domain.professional.Professional;
import com.alamano.core.domain.professional.ProfessionalConcurrentUpdateException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class ConnectProfessionalService implements ConnectProfessionalUseCase {
    static final int MAX_ATTEMPTS = 2;

    private final ProfessionalRepositoryPort repository;
    private final ProfessionalEventPublisherPort publisher;
    private final Clock clock;

    public ConnectProfessionalService(
            ProfessionalRepositoryPort repository, ProfessionalEventPublisherPort publisher, Clock clock) {
        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public Professional connect(String professionalId, double latitude, double longitude, String correlationId) {
        String effectiveCorrelationId = correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;
        GeoPoint location = new GeoPoint(latitude, longitude);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            Optional<Professional> saved = tryConnect(professionalId, location);
            if (saved.isPresent()) {
                // Solo se publica cuando el cambio ya quedó guardado.
                publisher.publishOnline(saved.get(), effectiveCorrelationId);
                return saved.get();
            }
        }
        throw new ProfessionalConcurrentUpdateException(professionalId);
    }

    /** Devuelve vacío si otro request creó o modificó al vendedor al mismo tiempo. */
    private Optional<Professional> tryConnect(String professionalId, GeoPoint location) {
        Instant now = clock.instant();
        Optional<Professional> current = repository.findById(professionalId);
        if (current.isEmpty()) {
            Professional created = Professional.connectFirstTime(professionalId, location, now);
            return repository.insert(created) ? Optional.of(created) : Optional.empty();
        }
        Professional actual = current.get();
        Professional updated = actual.goOnline(location, now);
        return repository.updateIfVersionMatches(updated, actual.version()) ? Optional.of(updated) : Optional.empty();
    }
}
