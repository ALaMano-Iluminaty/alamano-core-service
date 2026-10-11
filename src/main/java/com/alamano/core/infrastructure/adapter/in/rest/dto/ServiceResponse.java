package com.alamano.core.infrastructure.adapter.in.rest.dto;

import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ServiceResponse(
        UUID id,
        String professionalId,
        String clientId,
        ServiceStatus status,
        long version,
        Instant updatedAt,
        Set<String> allowedTransitions,
        Double destinationLatitude,
        Double destinationLongitude,
        Double lastLatitude,
        Double lastLongitude,
        Instant lastTrackedAt,
        Integer etaSeconds,
        ProfessionalSummaryResponse professional) {

    public static ServiceResponse from(
            Service service,
            Set<String> allowedTransitions,
            Integer etaSeconds,
            ProfessionalSummaryResponse professional) {
        return new ServiceResponse(
                service.id(),
                service.professionalId(),
                service.clientId(),
                service.status(),
                service.version(),
                service.updatedAt(),
                allowedTransitions,
                service.destinationLatitude(),
                service.destinationLongitude(),
                service.lastLatitude(),
                service.lastLongitude(),
                service.lastTrackedAt(),
                etaSeconds,
                professional);
    }
}
