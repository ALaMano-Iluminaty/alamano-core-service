package com.alamano.core.application.service;

import com.alamano.core.application.port.in.GetServiceUseCase;
import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceAccessDeniedException;
import com.alamano.core.domain.service.ServiceNotFoundException;
import java.util.UUID;

public class GetServiceService implements GetServiceUseCase {
    private final ServiceRepositoryPort repository;

    public GetServiceService(ServiceRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public Service get(UUID serviceId, String userId) {
        Service service = repository.findById(serviceId).orElseThrow(() -> new ServiceNotFoundException(serviceId));
        if (!service.isParticipant(userId)) {
            throw new ServiceAccessDeniedException();
        }
        return service;
    }
}
