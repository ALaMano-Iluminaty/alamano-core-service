package com.alamano.core.application.service;

import com.alamano.core.application.port.in.GetPromotionUseCase;
import com.alamano.core.application.port.out.PromotionCounterPort;
import com.alamano.core.application.port.out.PromotionRepositoryPort;
import com.alamano.core.domain.promotion.Promotion;
import com.alamano.core.domain.promotion.PromotionAvailability;
import com.alamano.core.domain.promotion.PromotionCounterUnavailableException;
import com.alamano.core.domain.promotion.PromotionNotFoundException;
import java.util.UUID;

public class GetPromotionService implements GetPromotionUseCase {
    private final PromotionRepositoryPort repository;
    private final PromotionCounterPort counter;

    public GetPromotionService(PromotionRepositoryPort repository, PromotionCounterPort counter) {
        this.repository = repository;
        this.counter = counter;
    }

    @Override
    public PromotionAvailability get(UUID promotionId) {
        Promotion promotion = repository.findById(promotionId)
                .orElseThrow(() -> new PromotionNotFoundException(promotionId));
        int available = counter.remaining(promotionId)
                .orElseThrow(() -> new PromotionCounterUnavailableException(promotionId, null));
        return new PromotionAvailability(promotion, available);
    }
}
