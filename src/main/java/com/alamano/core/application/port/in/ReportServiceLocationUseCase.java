package com.alamano.core.application.port.in;

import java.time.Instant;
import java.util.UUID;

public interface ReportServiceLocationUseCase {
    void report(UUID serviceId, String professionalId, double latitude, double longitude, Instant recordedAt,
            String correlationId);
}
