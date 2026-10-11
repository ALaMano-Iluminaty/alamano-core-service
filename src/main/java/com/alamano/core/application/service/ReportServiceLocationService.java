package com.alamano.core.application.service;

import com.alamano.core.application.port.in.ReportServiceLocationUseCase;
import com.alamano.core.application.port.in.UpdateTrackingUseCase;
import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceAccessDeniedException;
import com.alamano.core.domain.service.ServiceInactiveException;
import com.alamano.core.domain.service.ServiceNotFoundException;
import java.time.Instant;
import java.util.UUID;

public class ReportServiceLocationService implements ReportServiceLocationUseCase {
    private final ServiceRepositoryPort repository;
    private final UpdateTrackingUseCase tracking;

    public ReportServiceLocationService(ServiceRepositoryPort repository, UpdateTrackingUseCase tracking) {
        this.repository = repository;
        this.tracking = tracking;
    }

    @Override
    public void report(UUID serviceId, String professionalId, double latitude, double longitude, Instant recordedAt,
            String correlationId) {
        Service service = repository.findById(serviceId).orElseThrow(() -> new ServiceNotFoundException(serviceId));
        if (!service.professionalId().equals(professionalId)) {
            throw new ServiceAccessDeniedException();
        }
        if (service.isTerminal()) {
            throw new ServiceInactiveException(serviceId);
        }
        tracking.handleLocation(professionalId, serviceId.toString(), latitude, longitude, recordedAt, correlationId);
    }
}
