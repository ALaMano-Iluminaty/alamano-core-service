package com.alamano.core.application.port.out;

import com.alamano.core.domain.promotion.Promotion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepositoryPort {
    Promotion save(Promotion promotion);

    Optional<Promotion> findById(UUID promotionId);

    List<Promotion> findByProfessionalId(String professionalId);

    List<Promotion> findByProfessionalIds(Collection<String> professionalIds);

    /** Se usa solo para compensar cuando el contador de cupos no pudo iniciarse. */
    void deleteById(UUID promotionId);
}
