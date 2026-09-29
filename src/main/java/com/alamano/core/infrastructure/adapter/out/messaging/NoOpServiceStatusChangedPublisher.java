package com.alamano.core.infrastructure.adapter.out.messaging;

import com.alamano.core.application.port.out.ServiceStatusChangedPublisherPort;
import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class NoOpServiceStatusChangedPublisher implements ServiceStatusChangedPublisherPort {
    @Override
    public void publish(Service service, ServiceStatus previousStatus, String correlationId) {}
}
