package com.alamano.core.application.service;

import com.alamano.core.application.port.in.CreateServiceUseCase;
import com.alamano.core.application.port.out.ProfessionalRepositoryPort;
import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.application.port.out.ServiceStatusChangedPublisherPort;
import com.alamano.core.domain.professional.ProfessionalBusyException;
import com.alamano.core.domain.service.Service;
import java.time.Clock;
import java.util.UUID;

public class CreateServiceService implements CreateServiceUseCase {
    private final ServiceRepositoryPort repository;
    private final ProfessionalRepositoryPort professionals;
    private final Clock clock;
    private final ServiceStatusChangedPublisherPort publisher;

    public CreateServiceService(
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
    public Service createReserved(String professionalId, String clientId) {
        return createReserved(professionalId, clientId, null, null);
    }

    @Override
    public Service createReserved(String professionalId, String clientId, Double destinationLatitude,
            Double destinationLongitude) {
        if (!professionals.markBusyIfAvailable(professionalId)) {
            throw new ProfessionalBusyException(professionalId);
        }
        Service service = Service.createReserved(UUID.randomUUID(), professionalId, clientId, clock.instant(),
                destinationLatitude, destinationLongitude);
        Service saved = repository.save(service);
        publisher.publish(saved, null, null);
        return saved;
    }
}
