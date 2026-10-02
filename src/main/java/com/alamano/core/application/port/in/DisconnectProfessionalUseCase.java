package com.alamano.core.application.port.in;

import com.alamano.core.domain.professional.Professional;
import java.time.Instant;

public interface DisconnectProfessionalUseCase {
    /** El vendedor pide salir del mapa (botón "No disponible"). */
    Professional goOffline(String professionalId, String correlationId);

    /** El Gateway avisa que se cayó el socket del vendedor. Nunca lanza excepciones de negocio. */
    void handleConnectionLost(String professionalId, Instant lostAt, String correlationId);
}
