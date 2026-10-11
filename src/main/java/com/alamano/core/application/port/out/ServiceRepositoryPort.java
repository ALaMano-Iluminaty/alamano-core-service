package com.alamano.core.application.port.out;

import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceStatus;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRepositoryPort {
    Service save(Service service);

    Optional<Service> findById(UUID serviceId);

    boolean updateStatusIfMatches(
            UUID serviceId, ServiceStatus expectedStatus, long expectedVersion, Service updated);

    boolean hasActiveService(String professionalId);
}
