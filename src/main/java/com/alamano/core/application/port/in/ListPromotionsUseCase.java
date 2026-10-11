package com.alamano.core.application.port.in;

import com.alamano.core.domain.promotion.PromotionAvailability;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ListPromotionsUseCase {
    List<PromotionAvailability> byProfessional(String professionalId);

    List<PromotionAvailability> mine(String professionalId);

    Map<String, List<PromotionAvailability>> byProfessionals(Collection<String> professionalIds);
}
