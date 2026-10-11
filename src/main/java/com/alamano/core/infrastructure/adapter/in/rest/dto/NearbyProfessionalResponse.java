package com.alamano.core.infrastructure.adapter.in.rest.dto;

import com.alamano.core.domain.professional.NearbyProfessional;
import com.alamano.core.domain.promotion.PromotionAvailability;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record NearbyProfessionalResponse(
        String professionalId,
        double latitude,
        double longitude,
        double distanceKm,
        List<PromotionResponse> promotions) {
    public static NearbyProfessionalResponse from(
            NearbyProfessional professional, List<PromotionAvailability> promotions) {
        double roundedDistance = BigDecimal.valueOf(professional.distanceKm())
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        return new NearbyProfessionalResponse(
                professional.professionalId(),
                professional.location().latitude(),
                professional.location().longitude(),
                roundedDistance,
                promotions.stream().map(PromotionResponse::from).toList());
    }
}
