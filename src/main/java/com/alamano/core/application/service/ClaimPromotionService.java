package com.alamano.core.application.service;

import com.alamano.core.application.port.in.ClaimPromotionUseCase;
import com.alamano.core.application.port.out.PromotionClaimPort;
import com.alamano.core.application.port.out.PromotionCounterPort;
import com.alamano.core.application.port.out.PromotionRepositoryPort;
import com.alamano.core.domain.promotion.Promotion;
import com.alamano.core.domain.promotion.PromotionAlreadyClaimedException;
import com.alamano.core.domain.promotion.PromotionAvailability;
import com.alamano.core.domain.promotion.PromotionNoSlotsException;
import com.alamano.core.domain.promotion.PromotionNotFoundException;
import java.time.Clock;
import java.util.OptionalInt;
import java.util.UUID;

public class ClaimPromotionService implements ClaimPromotionUseCase {
    private final PromotionRepositoryPort repository;
    private final PromotionCounterPort counter;
    private final PromotionClaimPort claims;
    private final Clock clock;

    public ClaimPromotionService(
            PromotionRepositoryPort repository,
            PromotionCounterPort counter,
            PromotionClaimPort claims,
            Clock clock) {
        this.repository = repository;
        this.counter = counter;
        this.claims = claims;
        this.clock = clock;
    }

    @Override
    public PromotionAvailability claim(UUID promotionId, String clientId) {
        Promotion promotion = repository.findById(promotionId)
                .orElseThrow(() -> new PromotionNotFoundException(promotionId));
        OptionalInt remaining = counter.tryClaim(promotionId);
        if (remaining.isEmpty()) {
            throw new PromotionNoSlotsException(promotionId);
        }
        try {
            if (!claims.insertIfNew(promotionId, clientId, clock.instant())) {
                counter.restore(promotionId);
                throw new PromotionAlreadyClaimedException(promotionId);
            }
        } catch (PromotionAlreadyClaimedException e) {
            throw e;
        } catch (RuntimeException e) {
            counter.restore(promotionId);
            throw e;
        }
        return new PromotionAvailability(promotion, remaining.getAsInt());
    }
}
