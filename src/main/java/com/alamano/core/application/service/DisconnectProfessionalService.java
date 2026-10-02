package com.alamano.core.application.service;

import com.alamano.core.application.port.in.DisconnectProfessionalUseCase;
import com.alamano.core.application.port.out.ProfessionalEventPublisherPort;
import com.alamano.core.application.port.out.ProfessionalRepositoryPort;
import com.alamano.core.domain.professional.DisconnectReason;
import com.alamano.core.domain.professional.Professional;
import com.alamano.core.domain.professional.ProfessionalBusyException;
import com.alamano.core.domain.professional.ProfessionalConcurrentUpdateException;
import com.alamano.core.domain.professional.ProfessionalNotFoundException;
import com.alamano.core.domain.professional.ProfessionalStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisconnectProfessionalService implements DisconnectProfessionalUseCase {
    static final int MAX_ATTEMPTS = 2;
    private static final Logger log = LoggerFactory.getLogger(DisconnectProfessionalService.class);

    private final ProfessionalRepositoryPort repository;
    private final ProfessionalEventPublisherPort publisher;
    private final Clock clock;

    public DisconnectProfessionalService(
            ProfessionalRepositoryPort repository, ProfessionalEventPublisherPort publisher, Clock clock) {
        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public Professional goOffline(String professionalId, String correlationId) {
        String effectiveCorrelationId = effectiveCorrelationId(correlationId);
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            Professional current = repository.findById(professionalId)
                    .orElseThrow(() -> new ProfessionalNotFoundException(professionalId));
            if (current.status() == ProfessionalStatus.OFFLINE) return current;
            if (current.status() == ProfessionalStatus.BUSY) throw new ProfessionalBusyException(professionalId);
            Professional offline = current.goOffline(clock.instant());
            if (repository.updateIfVersionMatches(offline, current.version())) {
                publisher.publishDisconnected(offline, DisconnectReason.MANUAL, effectiveCorrelationId);
                return offline;
            }
        }
        throw new ProfessionalConcurrentUpdateException(professionalId);
    }

    @Override
    public void handleConnectionLost(String professionalId, Instant lostAt, String correlationId) {
        String effectiveCorrelationId = effectiveCorrelationId(correlationId);
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            Optional<Professional> found = repository.findById(professionalId);
            if (found.isEmpty()) {
                log.info("Se ignora la desconexión: no existe el vendedor {}.", professionalId);
                return;
            }
            Professional current = found.get();
            if (current.status() == ProfessionalStatus.OFFLINE) {
                log.info("Se ignora la desconexión: el vendedor {} ya está OFFLINE.", professionalId);
                return;
            }
            if (current.status() == ProfessionalStatus.BUSY) {
                log.info("Se ignora la desconexión: el vendedor {} tiene un servicio en curso.", professionalId);
                return;
            }
            if (current.wentOnlineAfter(lostAt)) {
                log.info("Se ignora la desconexión: el vendedor {} se reconectó después de la pérdida.", professionalId);
                return;
            }
            Professional offline = current.goOffline(clock.instant());
            if (repository.updateIfVersionMatches(offline, current.version())) {
                publisher.publishDisconnected(offline, DisconnectReason.CONNECTION_LOST, effectiveCorrelationId);
                return;
            }
        }
        log.warn("No se pudo marcar OFFLINE al vendedor {} tras {} intentos por un conflicto de versión.",
                professionalId, MAX_ATTEMPTS);
    }

    private String effectiveCorrelationId(String correlationId) {
        return correlationId == null || correlationId.isBlank() ? UUID.randomUUID().toString() : correlationId;
    }
}
