package com.alamano.core.application.port.out;

import com.alamano.core.domain.promotion.Promotion;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepositoryPort {
    Promotion save(Promotion promotion);

    Optional<Promotion> findById(UUID promotionId);

    /** Se usa solo para compensar cuando el contador de cupos no pudo iniciarse. */
    void deleteById(UUID promotionId);
}
