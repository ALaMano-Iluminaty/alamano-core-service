package com.alamano.core.application.port.in;

import com.alamano.core.domain.promotion.PromotionAvailability;
import java.util.UUID;

public interface GetPromotionUseCase {
    PromotionAvailability get(UUID promotionId);
}
