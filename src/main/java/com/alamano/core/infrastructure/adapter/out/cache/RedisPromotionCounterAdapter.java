package com.alamano.core.infrastructure.adapter.out.cache;

import com.alamano.core.application.port.out.PromotionCounterPort;
import com.alamano.core.domain.promotion.PromotionCounterUnavailableException;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class RedisPromotionCounterAdapter implements PromotionCounterPort {
    private static final DefaultRedisScript<Long> CLAIM_SCRIPT = new DefaultRedisScript<>();

    static {
        CLAIM_SCRIPT.setResultType(Long.class);
        CLAIM_SCRIPT.setScriptText(
                """
                local current = redis.call('get', KEYS[1])
                if current == false then return -2 end
                current = tonumber(current)
                if current == nil or current <= 0 then return -1 end
                return redis.call('decr', KEYS[1])
                """);
    }

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

    @Override
    public OptionalInt tryClaim(UUID promotionId) {
        try {
            Long result = redis.execute(CLAIM_SCRIPT, List.of(key(promotionId)));
            if (result == null || result < 0) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(result.intValue());
        } catch (DataAccessException e) {
            throw new PromotionCounterUnavailableException(promotionId, e);
        }
    }

    @Override
    public void restore(UUID promotionId) {
        try {
            redis.opsForValue().increment(key(promotionId));
        } catch (DataAccessException e) {
            throw new PromotionCounterUnavailableException(promotionId, e);
        }
    }
}
