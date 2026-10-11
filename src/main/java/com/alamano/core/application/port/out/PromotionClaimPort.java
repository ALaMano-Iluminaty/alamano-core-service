package com.alamano.core.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface PromotionClaimPort {
    /** False si ese cliente ya tomó esta promoción. */
    boolean insertIfNew(UUID promotionId, String clientId, Instant claimedAt);
}
