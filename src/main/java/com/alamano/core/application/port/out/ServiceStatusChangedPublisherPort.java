package com.alamano.core.application.port.out;

import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceStatus;

public interface ServiceStatusChangedPublisherPort {
    void publish(Service service, ServiceStatus previousStatus, String correlationId);
}
