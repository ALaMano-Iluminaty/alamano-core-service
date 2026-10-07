package com.alamano.core.application.port.out;

import com.alamano.core.domain.professional.Professional;
import com.alamano.core.domain.professional.DisconnectReason;

public interface ProfessionalEventPublisherPort {
    void publishOnline(Professional professional, String correlationId);

    void publishDisconnected(Professional professional, DisconnectReason reason, String correlationId);
}
