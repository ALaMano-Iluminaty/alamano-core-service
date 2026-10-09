package com.alamano.core.infrastructure.adapter.in.rest.dto;

import com.alamano.core.domain.promotion.PromotionAvailability;
import java.time.Instant;
import java.util.UUID;

public record PromotionResponse(
        UUID id, String professionalId, String description, int totalSlots, int availableSlots, Instant createdAt) {

    public static PromotionResponse from(PromotionAvailability availability) {
        return new PromotionResponse(
                availability.promotion().id(),
                availability.promotion().professionalId(),
                availability.promotion().description(),
                availability.promotion().totalSlots(),
                availability.availableSlots(),
                availability.promotion().createdAt());
    }
}
