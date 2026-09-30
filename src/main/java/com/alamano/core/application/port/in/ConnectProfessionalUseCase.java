package com.alamano.core.application.port.in;

import com.alamano.core.domain.professional.Professional;

public interface ConnectProfessionalUseCase {
    Professional connect(String professionalId, double latitude, double longitude, String correlationId);
}
