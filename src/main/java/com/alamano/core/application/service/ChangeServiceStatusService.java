package com.alamano.core.application.service;

import com.alamano.core.application.port.in.ChangeServiceStatusUseCase;
import com.alamano.core.application.port.out.ProfessionalRepositoryPort;
import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.application.port.out.ServiceStatusChangedPublisherPort;
import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceAccessDeniedException;
import com.alamano.core.domain.service.ServiceNotFoundException;
import com.alamano.core.domain.service.ServiceStatus;
import com.alamano.core.domain.service.ServiceStatusConflictException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class ChangeServiceStatusService implements ChangeServiceStatusUseCase {
    private final ServiceRepositoryPort repository;
    private final ProfessionalRepositoryPort professionals;
    private final ServiceStatusChangedPublisherPort publisher;
    private final Clock clock;

    public ChangeServiceStatusService(
            ServiceRepositoryPort repository,
            ProfessionalRepositoryPort professionals,
            ServiceStatusChangedPublisherPort publisher,
            Clock clock) {
        this.repository = repository;
        this.professionals = professionals;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public Service changeStatus(UUID serviceId, ServiceStatus targetStatus, String userId, String correlationId) {
        Service current = repository.findById(serviceId).orElseThrow(() -> new ServiceNotFoundException(serviceId));
        boolean professional = current.professionalId().equals(userId);
        boolean clientCancellation = current.clientId().equals(userId) && targetStatus == ServiceStatus.CANCELLED;
        if (!professional && !clientCancellation) {
            throw new ServiceAccessDeniedException();
        }
        ServiceStatus previousStatus = current.status();
        Service transitioned = current.transitionTo(targetStatus);
        Instant now = clock.instant();
        Service updated = transitioned.withUpdatedAt(now);

        boolean applied = repository.updateStatusIfMatches(
                serviceId, previousStatus, current.version(), updated);
        if (!applied) {
            throw new ServiceStatusConflictException(serviceId);
        }

        if (updated.isTerminal() && !repository.hasActiveService(updated.professionalId())) {
            professionals.releaseIfBusy(updated.professionalId());
        }

        publisher.publish(updated, previousStatus, correlationId);
        return updated;
    }
}
