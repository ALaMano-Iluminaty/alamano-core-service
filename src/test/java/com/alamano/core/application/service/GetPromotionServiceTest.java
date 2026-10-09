package com.alamano.core.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alamano.core.application.port.out.PromotionCounterPort;
import com.alamano.core.application.port.out.PromotionRepositoryPort;
import com.alamano.core.domain.promotion.Promotion;
import com.alamano.core.domain.promotion.PromotionAvailability;
import com.alamano.core.domain.promotion.PromotionCounterUnavailableException;
import com.alamano.core.domain.promotion.PromotionNotFoundException;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetPromotionServiceTest {
    private final PromotionRepositoryPort repository = mock(PromotionRepositoryPort.class);
    private final PromotionCounterPort counter = mock(PromotionCounterPort.class);
    private final GetPromotionService service = new GetPromotionService(repository, counter);
    private final UUID id = UUID.randomUUID();
    private final Promotion promotion =
            Promotion.publish(id, "pro-1", "2x1 en cortes", 50, Instant.parse("2026-10-09T12:00:00Z"));

    @Test
    void returnsThePromotionWithTheSlotsLeftInTheCounter() {
        when(repository.findById(id)).thenReturn(Optional.of(promotion));
        when(counter.remaining(id)).thenReturn(OptionalInt.of(37));

        PromotionAvailability result = service.get(id);

        assertEquals(promotion, result.promotion());
        assertEquals(37, result.availableSlots());
    }

    @Test
    void unknownPromotionIsNotFound() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(PromotionNotFoundException.class, () -> service.get(id));
    }

    @Test
    void missingCounterIsReportedAsUnavailable() {
        when(repository.findById(id)).thenReturn(Optional.of(promotion));
        when(counter.remaining(id)).thenReturn(OptionalInt.empty());

        assertThrows(PromotionCounterUnavailableException.class, () -> service.get(id));
    }
}
