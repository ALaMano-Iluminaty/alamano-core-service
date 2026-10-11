package com.alamano.core.domain.promotion;

import java.util.UUID;

public class PromotionNoSlotsException extends RuntimeException {
    private final UUID promotionId;

    public PromotionNoSlotsException(UUID promotionId) {
        super("Ya no quedan cupos para la promoción " + promotionId);
        this.promotionId = promotionId;
    }

    public UUID promotionId() {
        return promotionId;
    }
}
