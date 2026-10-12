package com.alamano.core.application.port.out;

import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRepositoryPort {
    Service save(Service service);

    Optional<Service> findById(UUID serviceId);

    boolean updateStatusIfMatches(
            UUID serviceId, ServiceStatus expectedStatus, long expectedVersion, Service updated);

    boolean hasActiveService(String professionalId);

    /** Servicios no terminales del profesional, del más reciente al más antiguo. */
    List<Service> findActiveByProfessionalId(String professionalId);

    /** Servicios no terminales del cliente, del más reciente al más antiguo. */
    List<Service> findActiveByClientId(String clientId);
}
