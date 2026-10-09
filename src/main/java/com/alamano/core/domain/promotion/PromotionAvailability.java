package com.alamano.core.domain.promotion;

/** Una promoción junto con los cupos que quedan según el contador atómico. */
public record PromotionAvailability(Promotion promotion, int availableSlots) {}
