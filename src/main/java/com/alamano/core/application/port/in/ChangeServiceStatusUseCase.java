package com.alamano.core.application.port.in;

import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceStatus;
import java.util.UUID;

public interface ChangeServiceStatusUseCase {
    Service changeStatus(UUID serviceId, ServiceStatus targetStatus, String correlationId);
}
