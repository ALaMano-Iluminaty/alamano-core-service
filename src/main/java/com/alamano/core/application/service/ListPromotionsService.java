package com.alamano.core.application.service;

import com.alamano.core.application.port.in.ListPromotionsUseCase;
import com.alamano.core.application.port.out.PromotionCounterPort;
import com.alamano.core.application.port.out.PromotionRepositoryPort;
import com.alamano.core.domain.promotion.Promotion;
import com.alamano.core.domain.promotion.PromotionAvailability;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ListPromotionsService implements ListPromotionsUseCase {
    private final PromotionRepositoryPort repository;
    private final PromotionCounterPort counter;

    public ListPromotionsService(PromotionRepositoryPort repository, PromotionCounterPort counter) {
        this.repository = repository;
        this.counter = counter;
    }

    @Override
    public List<PromotionAvailability> byProfessional(String professionalId) {
        return withSlots(repository.findByProfessionalId(professionalId));
    }

    @Override
    public List<PromotionAvailability> mine(String professionalId) {
        return byProfessional(professionalId);
    }

    @Override
    public Map<String, List<PromotionAvailability>> byProfessionals(Collection<String> professionalIds) {
        if (professionalIds.isEmpty()) {
            return Map.of();
        }
        return withSlots(repository.findByProfessionalIds(professionalIds)).stream()
                .collect(Collectors.groupingBy(availability -> availability.promotion().professionalId()));
    }

    private List<PromotionAvailability> withSlots(List<Promotion> promotions) {
        return promotions.stream()
                .map(promotion -> new PromotionAvailability(
                        promotion, counter.remaining(promotion.id()).orElse(0)))
                .toList();
    }
}
