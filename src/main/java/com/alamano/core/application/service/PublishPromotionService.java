package com.alamano.core.application.service;

import com.alamano.core.application.port.in.PublishPromotionUseCase;
import com.alamano.core.application.port.out.PromotionCounterPort;
import com.alamano.core.application.port.out.PromotionRepositoryPort;
import com.alamano.core.domain.promotion.Promotion;
import com.alamano.core.domain.promotion.PromotionCounterUnavailableException;
import java.time.Clock;
import java.util.UUID;

public class PublishPromotionService implements PublishPromotionUseCase {
    private final PromotionRepositoryPort repository;
    private final PromotionCounterPort counter;
    private final Clock clock;

    public PublishPromotionService(PromotionRepositoryPort repository, PromotionCounterPort counter, Clock clock) {
        this.repository = repository;
        this.counter = counter;
        this.clock = clock;
    }

    @Override
    public Promotion publish(String professionalId, String description, int totalSlots) {
        // Las validaciones viven en el dominio: si algo es inválido no se toca Postgres ni Redis.
        Promotion promotion =
                Promotion.publish(UUID.randomUUID(), professionalId, description, totalSlots, clock.instant());

        repository.save(promotion);
        try {
            counter.initialize(promotion.id(), promotion.totalSlots());
        } catch (RuntimeException failure) {
            // Postgres y Redis no comparten transacción: si el contador falla se deshace el guardado
            // para no dejar una promoción visible sin cupos.
            compensate(promotion, failure);
            throw failure instanceof PromotionCounterUnavailableException unavailable
                    ? unavailable
                    : new PromotionCounterUnavailableException(promotion.id(), failure);
        }
        return promotion;
    }

    private void compensate(Promotion promotion, RuntimeException failure) {
        try {
            repository.deleteById(promotion.id());
        } catch (RuntimeException deleteFailure) {
            failure.addSuppressed(deleteFailure);
        }
    }
}
