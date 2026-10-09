package com.alamano.core.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.alamano.core.application.port.out.PromotionCounterPort;
import com.alamano.core.application.port.out.PromotionRepositoryPort;
import com.alamano.core.domain.promotion.InvalidPromotionException;
import com.alamano.core.domain.promotion.Promotion;
import com.alamano.core.domain.promotion.PromotionCounterUnavailableException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class PublishPromotionServiceTest {
    private PromotionRepositoryPort repository;
    private PromotionCounterPort counter;
    private PublishPromotionService service;

    @BeforeEach
    void setUp() {
        repository = mock(PromotionRepositoryPort.class);
        counter = mock(PromotionCounterPort.class);
        Clock clock = Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC);
        when(repository.save(any(Promotion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new PublishPromotionService(repository, counter, clock);
    }

    @Test
    void savesBeforeInitializingTheCounterWithTheTotalSlots() {
        Promotion promotion = service.publish("pro-1", "2x1 en cortes", 50);

        InOrder order = inOrder(repository, counter);
        order.verify(repository).save(promotion);
        order.verify(counter).initialize(promotion.id(), 50);
        assertEquals("pro-1", promotion.professionalId());
        assertEquals(50, promotion.totalSlots());
    }

    @Test
    void undoesTheSaveWhenTheCounterCannotBeInitialized() {
        doThrow(new IllegalStateException("redis caído")).when(counter).initialize(any(UUID.class), anyInt());

        assertThrows(PromotionCounterUnavailableException.class, () -> service.publish("pro-1", "2x1", 50));

        ArgumentCaptor<Promotion> saved = ArgumentCaptor.forClass(Promotion.class);
        verify(repository).save(saved.capture());
        verify(repository).deleteById(saved.getValue().id());
    }

    @Test
    void keepsTheOriginalFailureEvenIfTheCompensationFails() {
        PromotionCounterUnavailableException original = new PromotionCounterUnavailableException(UUID.randomUUID(), null);
        doThrow(original).when(counter).initialize(any(UUID.class), anyInt());
        doThrow(new IllegalStateException("postgres caído")).when(repository).deleteById(any(UUID.class));

        PromotionCounterUnavailableException thrown =
                assertThrows(PromotionCounterUnavailableException.class, () -> service.publish("pro-1", "2x1", 50));

        assertSame(original, thrown);
        assertEquals(1, thrown.getSuppressed().length);
    }

    @Test
    void doesNotTouchTheCounterWhenTheSaveFails() {
        when(repository.save(any(Promotion.class))).thenThrow(new IllegalStateException("postgres caído"));

        assertThrows(IllegalStateException.class, () -> service.publish("pro-1", "2x1", 50));

        verifyNoInteractions(counter);
        verify(repository, never()).deleteById(any(UUID.class));
    }

    @Test
    void invalidPromotionDoesNotTouchPostgresNorRedis() {
        assertThrows(InvalidPromotionException.class, () -> service.publish("pro-1", "2x1", 0));
        assertThrows(InvalidPromotionException.class, () -> service.publish("pro-1", " ", 5));

        verifyNoInteractions(repository, counter);
    }
}
