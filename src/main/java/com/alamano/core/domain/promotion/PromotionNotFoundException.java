package com.alamano.core.domain.promotion;

import java.util.UUID;

public class PromotionNotFoundException extends RuntimeException {
    private final UUID promotionId;

    public PromotionNotFoundException(UUID promotionId) {
        super("Promoción no encontrada: " + promotionId);
        this.promotionId = promotionId;
    }

    public UUID promotionId() {
        return promotionId;
    }
}
