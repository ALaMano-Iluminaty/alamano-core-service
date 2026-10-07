package com.alamano.core.infrastructure.adapter.out.messaging;

import com.alamano.core.application.port.out.ProfessionalEventPublisherPort;
import com.alamano.core.domain.professional.Professional;
import com.alamano.core.domain.professional.DisconnectReason;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class NoOpProfessionalEventPublisher implements ProfessionalEventPublisherPort {
    @Override
    public void publishOnline(Professional professional, String correlationId) {}

    @Override
    public void publishDisconnected(Professional professional, DisconnectReason reason, String correlationId) {}
}
