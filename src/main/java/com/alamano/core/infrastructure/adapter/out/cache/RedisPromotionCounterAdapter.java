package com.alamano.core.infrastructure.adapter.out.cache;

import com.alamano.core.application.port.out.PromotionCounterPort;
import com.alamano.core.domain.promotion.PromotionCounterUnavailableException;
import java.util.OptionalInt;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class RedisPromotionCounterAdapter implements PromotionCounterPort {
    private final StringRedisTemplate redis;

    public RedisPromotionCounterAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Clave del contador: promotion:{id}:cupos. HU11 usa la misma clave en su script Lua. */
    public static String key(UUID promotionId) {
        return "promotion:" + promotionId + ":cupos";
    }

    @Override
    public void initialize(UUID promotionId, int slots) {
        try {
            redis.opsForValue().set(key(promotionId), Integer.toString(slots));
        } catch (DataAccessException e) {
            throw new PromotionCounterUnavailableException(promotionId, e);
        }
    }

    @Override
    public OptionalInt remaining(UUID promotionId) {
        try {
            String value = redis.opsForValue().get(key(promotionId));
            return value == null ? OptionalInt.empty() : OptionalInt.of(Integer.parseInt(value));
        } catch (DataAccessException | NumberFormatException e) {
            throw new PromotionCounterUnavailableException(promotionId, e);
        }
    }
}
