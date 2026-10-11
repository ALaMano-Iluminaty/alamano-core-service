package com.alamano.core.infrastructure.adapter.out.persistence;

import com.alamano.core.application.port.out.PromotionClaimPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PromotionClaimJdbcAdapter implements PromotionClaimPort {
    private final JdbcTemplate jdbc;

    public PromotionClaimJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean insertIfNew(UUID promotionId, String clientId, Instant claimedAt) {
        try {
            jdbc.update(
                    """
                    INSERT INTO promotion_claims (promotion_id, client_id, claimed_at)
                    VALUES (?, ?, ?)
                    """,
                    promotionId,
                    clientId,
                    Timestamp.from(claimedAt));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
