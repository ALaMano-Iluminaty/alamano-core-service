package com.alamano.core.infrastructure.adapter.out.cache;

import com.alamano.core.application.port.out.PromotionCounterPort;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Contador en memoria para el perfil test, igual que los NoOp de mensajería: no necesita Redis. */
@Component
@Profile("test")
public class InMemoryPromotionCounterAdapter implements PromotionCounterPort {
    private final Map<UUID, Integer> slots = new ConcurrentHashMap<>();

    @Override
    public void initialize(UUID promotionId, int initialSlots) {
        slots.put(promotionId, initialSlots);
    }

    @Override
    public OptionalInt remaining(UUID promotionId) {
        Integer value = slots.get(promotionId);
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    @Override
    public OptionalInt tryClaim(UUID promotionId) {
        final Integer[] remaining = new Integer[1];
        slots.compute(promotionId, (id, current) -> {
            if (current == null || current <= 0) {
                remaining[0] = null;
                return current;
            }
            int next = current - 1;
            remaining[0] = next;
            return next;
        });
        return remaining[0] == null ? OptionalInt.empty() : OptionalInt.of(remaining[0]);
    }

    @Override
    public void restore(UUID promotionId) {
        slots.compute(promotionId, (id, current) -> current == null ? 1 : current + 1);
    }
}
