package com.alamano.core.domain.promotion;

import java.time.Instant;
import java.util.UUID;

/** Promoción de un vendedor con un número limitado de cupos (HU12). */
public record Promotion(UUID id, String professionalId, String description, int totalSlots, Instant createdAt) {
    public static final int MAX_DESCRIPTION_LENGTH = 500;
    public static final int MAX_SLOTS = 10_000;

    public static Promotion publish(UUID id, String professionalId, String description, int totalSlots, Instant now) {
        if (professionalId == null || professionalId.isBlank()) {
            throw new InvalidPromotionException("La promoción necesita un vendedor");
        }
        if (description == null || description.isBlank()) {
            throw new InvalidPromotionException("La descripción es obligatoria");
        }
        String trimmed = description.trim();
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidPromotionException(
                    "La descripción no puede superar " + MAX_DESCRIPTION_LENGTH + " caracteres");
        }
        if (totalSlots < 1 || totalSlots > MAX_SLOTS) {
            throw new InvalidPromotionException("Los cupos deben estar entre 1 y " + MAX_SLOTS);
        }
        return new Promotion(id, professionalId, trimmed, totalSlots, now);
    }
}
