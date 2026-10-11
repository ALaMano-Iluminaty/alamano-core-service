package com.alamano.core.infrastructure.adapter.out.persistence;

import com.alamano.core.application.port.out.PromotionRepositoryPort;
import com.alamano.core.domain.promotion.Promotion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class PromotionJdbcAdapter implements PromotionRepositoryPort {
    private static final RowMapper<Promotion> ROW_MAPPER = PromotionJdbcAdapter::mapRow;
    private final JdbcTemplate jdbc;

    public PromotionJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Promotion save(Promotion promotion) {
        jdbc.update(
                """
                INSERT INTO promotions (id, professional_id, description, total_slots, created_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                promotion.id(),
                promotion.professionalId(),
                promotion.description(),
                promotion.totalSlots(),
                Timestamp.from(promotion.createdAt()));
        return promotion;
    }

    @Override
    public Optional<Promotion> findById(UUID promotionId) {
        return jdbc.query(
                        """
                        SELECT id, professional_id, description, total_slots, created_at
                        FROM promotions WHERE id = ?
                        """,
                        ROW_MAPPER,
                        promotionId)
                .stream()
                .findFirst();
    }

    @Override
    public List<Promotion> findByProfessionalId(String professionalId) {
        return jdbc.query(
                """
                SELECT id, professional_id, description, total_slots, created_at
                FROM promotions WHERE professional_id = ? ORDER BY created_at DESC
                """,
                ROW_MAPPER,
                professionalId);
    }

    @Override
    public List<Promotion> findByProfessionalIds(Collection<String> professionalIds) {
        if (professionalIds.isEmpty()) {
            return List.of();
        }
        String placeholders = professionalIds.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("");
        return jdbc.query(
                """
                SELECT id, professional_id, description, total_slots, created_at
                FROM promotions WHERE professional_id IN (%s) ORDER BY created_at DESC
                """.formatted(placeholders),
                ROW_MAPPER,
                professionalIds.toArray());
    }

    @Override
    public void deleteById(UUID promotionId) {
        jdbc.update("DELETE FROM promotions WHERE id = ?", promotionId);
    }

    private static Promotion mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Promotion(
                rs.getObject("id", UUID.class),
                rs.getString("professional_id"),
                rs.getString("description"),
                rs.getInt("total_slots"),
                rs.getTimestamp("created_at").toInstant());
    }
}
