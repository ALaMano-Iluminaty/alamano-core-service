package com.alamano.core.application.port.out;

import com.alamano.core.domain.professional.Professional;

public interface ProfessionalEventPublisherPort {
    void publishOnline(Professional professional, String correlationId);
}
