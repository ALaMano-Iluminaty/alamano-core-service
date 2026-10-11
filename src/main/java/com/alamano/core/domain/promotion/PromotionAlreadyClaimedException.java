package com.alamano.core.domain.promotion;

import java.util.UUID;

public class PromotionAlreadyClaimedException extends RuntimeException {
    private final UUID promotionId;

    public PromotionAlreadyClaimedException(UUID promotionId) {
        super("Ya tomaste esta promoción");
        this.promotionId = promotionId;
    }

    public UUID promotionId() {
        return promotionId;
    }
}
