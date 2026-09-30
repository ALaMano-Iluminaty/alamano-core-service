package com.alamano.core.infrastructure.adapter.in.rest.dto;

import com.alamano.core.domain.professional.NearbyProfessional;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record NearbyProfessionalResponse(String professionalId, double latitude, double longitude, double distanceKm) {
    public static NearbyProfessionalResponse from(NearbyProfessional professional) {
        double roundedDistance = BigDecimal.valueOf(professional.distanceKm())
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        return new NearbyProfessionalResponse(
                professional.professionalId(),
                professional.location().latitude(),
                professional.location().longitude(),
                roundedDistance);
    }
}
