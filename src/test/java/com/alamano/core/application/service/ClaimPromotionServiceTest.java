package com.alamano.core.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alamano.core.application.port.out.PromotionClaimPort;
import com.alamano.core.application.port.out.PromotionCounterPort;
import com.alamano.core.application.port.out.PromotionRepositoryPort;
import com.alamano.core.domain.promotion.Promotion;
import com.alamano.core.domain.promotion.PromotionAlreadyClaimedException;
import com.alamano.core.domain.promotion.PromotionAvailability;
import com.alamano.core.domain.promotion.PromotionNoSlotsException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClaimPromotionServiceTest {
    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private final Promotion promotion = Promotion.publish(ID, "pro-1", "2x1", 2, Instant.parse("2026-10-10T12:00:00Z"));
    private PromotionRepositoryPort repository;
    private PromotionCounterPort counter;
    private PromotionClaimPort claims;
    private ClaimPromotionService useCase;

    @BeforeEach
    void setUp() {
        repository = mock(PromotionRepositoryPort.class);
        counter = mock(PromotionCounterPort.class);
        claims = mock(PromotionClaimPort.class);
        useCase = new ClaimPromotionService(
                repository, counter, claims, Clock.fixed(Instant.parse("2026-10-10T13:00:00Z"), ZoneOffset.UTC));
        when(repository.findById(ID)).thenReturn(Optional.of(promotion));
    }

    @Test
    void claimsSlotAndPersistsClient() {
        when(counter.tryClaim(ID)).thenReturn(OptionalInt.of(1));
        when(claims.insertIfNew(eq(ID), eq("cli-1"), any())).thenReturn(true);

        PromotionAvailability result = useCase.claim(ID, "cli-1");

        assertEquals(1, result.availableSlots());
        verify(counter, never()).restore(ID);
    }

    @Test
    void restoresCounterWhenClientAlreadyClaimed() {
        when(counter.tryClaim(ID)).thenReturn(OptionalInt.of(1));
        when(claims.insertIfNew(eq(ID), eq("cli-1"), any())).thenReturn(false);

        assertThrows(PromotionAlreadyClaimedException.class, () -> useCase.claim(ID, "cli-1"));
        verify(counter).restore(ID);
    }

    @Test
    void rejectsWhenNoSlotsRemain() {
        when(counter.tryClaim(ID)).thenReturn(OptionalInt.empty());

        assertThrows(PromotionNoSlotsException.class, () -> useCase.claim(ID, "cli-1"));
        verify(claims, never()).insertIfNew(any(), any(), any());
    }
}
