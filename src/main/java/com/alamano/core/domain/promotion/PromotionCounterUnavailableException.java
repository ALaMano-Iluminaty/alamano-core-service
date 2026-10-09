package com.alamano.core.domain.promotion;

import java.util.UUID;

/** El contador de cupos (Redis) no respondió o no tiene valor para la promoción. */
public class PromotionCounterUnavailableException extends RuntimeException {
    private final UUID promotionId;

    public PromotionCounterUnavailableException(UUID promotionId, Throwable cause) {
        super("No se pudo leer o iniciar el contador de cupos de la promoción: " + promotionId, cause);
        this.promotionId = promotionId;
    }

    public UUID promotionId() {
        return promotionId;
    }
}
