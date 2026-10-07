package com.alamano.core.application.service;

import com.alamano.core.application.port.in.CreateServiceUseCase;
import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.domain.service.Service;
import java.time.Clock;
import java.util.UUID;

public class CreateServiceService implements CreateServiceUseCase {
    private final ServiceRepositoryPort repository;
    private final Clock clock;

    public CreateServiceService(ServiceRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Service createReserved(String professionalId, String clientId) {
        return createReserved(professionalId, clientId, null, null);
    }

    @Override
    public Service createReserved(String professionalId, String clientId, Double destinationLatitude,
            Double destinationLongitude) {
        Service service = Service.createReserved(UUID.randomUUID(), professionalId, clientId, clock.instant(),
                destinationLatitude, destinationLongitude);
        return repository.save(service);
    }
}
