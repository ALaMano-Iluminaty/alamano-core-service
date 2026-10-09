package com.alamano.core.domain.promotion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PromotionTest {
    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");

    @Test
    void publishesWithTrimmedDescription() {
        UUID id = UUID.randomUUID();

        Promotion promotion = Promotion.publish(id, "pro-1", "  2x1 en cortes  ", 50, NOW);

        assertEquals(id, promotion.id());
        assertEquals("pro-1", promotion.professionalId());
        assertEquals("2x1 en cortes", promotion.description());
        assertEquals(50, promotion.totalSlots());
        assertEquals(NOW, promotion.createdAt());
    }

    @Test
    void acceptsTheLimitsOfTheSlotRange() {
        assertEquals(1, Promotion.publish(UUID.randomUUID(), "pro-1", "x", 1, NOW).totalSlots());
        assertEquals(
                Promotion.MAX_SLOTS,
                Promotion.publish(UUID.randomUUID(), "pro-1", "x", Promotion.MAX_SLOTS, NOW).totalSlots());
    }

    @Test
    void rejectsZeroNegativeAndTooManySlots() {
        assertThrows(InvalidPromotionException.class, () -> Promotion.publish(UUID.randomUUID(), "pro-1", "x", 0, NOW));
        assertThrows(InvalidPromotionException.class, () -> Promotion.publish(UUID.randomUUID(), "pro-1", "x", -3, NOW));
        assertThrows(
                InvalidPromotionException.class,
                () -> Promotion.publish(UUID.randomUUID(), "pro-1", "x", Promotion.MAX_SLOTS + 1, NOW));
    }

    @Test
    void rejectsBlankOrMissingDescription() {
        assertThrows(InvalidPromotionException.class, () -> Promotion.publish(UUID.randomUUID(), "pro-1", "   ", 5, NOW));
        assertThrows(InvalidPromotionException.class, () -> Promotion.publish(UUID.randomUUID(), "pro-1", null, 5, NOW));
    }

    @Test
    void rejectsTooLongDescription() {
        String tooLong = "a".repeat(Promotion.MAX_DESCRIPTION_LENGTH + 1);
        assertThrows(InvalidPromotionException.class, () -> Promotion.publish(UUID.randomUUID(), "pro-1", tooLong, 5, NOW));
    }

    @Test
    void rejectsMissingProfessional() {
        assertThrows(InvalidPromotionException.class, () -> Promotion.publish(UUID.randomUUID(), " ", "x", 5, NOW));
        assertThrows(InvalidPromotionException.class, () -> Promotion.publish(UUID.randomUUID(), null, "x", 5, NOW));
    }
}
