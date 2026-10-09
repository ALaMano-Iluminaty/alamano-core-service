package com.alamano.core.application.port.in;

import com.alamano.core.domain.promotion.Promotion;

public interface PublishPromotionUseCase {
    Promotion publish(String professionalId, String description, int totalSlots);
}
