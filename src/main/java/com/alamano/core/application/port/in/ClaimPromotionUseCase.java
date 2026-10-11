package com.alamano.core.application.port.in;

import com.alamano.core.domain.promotion.PromotionAvailability;
import java.util.UUID;

public interface ClaimPromotionUseCase {
    PromotionAvailability claim(UUID promotionId, String clientId);
}
