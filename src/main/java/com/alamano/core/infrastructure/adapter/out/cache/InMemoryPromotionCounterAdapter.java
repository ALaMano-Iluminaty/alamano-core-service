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
}
