package com.alamano.core.application.port.in;

import java.time.Instant;

public interface UpdateTrackingUseCase {
    void handleLocation(String professionalId, String serviceId, double latitude, double longitude,
            Instant recordedAt, String correlationId);
}
