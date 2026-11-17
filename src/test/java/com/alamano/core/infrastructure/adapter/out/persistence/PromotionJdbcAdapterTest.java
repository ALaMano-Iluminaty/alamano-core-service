package com.alamano.core.infrastructure.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alamano.core.domain.promotion.Promotion;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** HU-Q5: la consulta por varios vendedores usa parametros con nombre, no SQL armado con texto. */
@SpringBootTest
@ActiveProfiles("test")
class PromotionJdbcAdapterTest {
    @Autowired
    private PromotionJdbcAdapter adapter;

    private final UUID primera = UUID.randomUUID();
    private final UUID segunda = UUID.randomUUID();
    private final UUID ajena = UUID.randomUUID();

    @AfterEach
    void limpiar() {
        adapter.deleteById(primera);
        adapter.deleteById(segunda);
        adapter.deleteById(ajena);
    }

    @Test
    void devuelveSoloLasPromocionesDeLosVendedoresPedidos() {
        adapter.save(new Promotion(primera, "pro-q5-a", "corte", 5, Instant.now()));
        adapter.save(new Promotion(segunda, "pro-q5-b", "barba", 3, Instant.now()));
        adapter.save(new Promotion(ajena, "pro-q5-c", "otra", 2, Instant.now()));

        List<Promotion> encontradas = adapter.findByProfessionalIds(List.of("pro-q5-a", "pro-q5-b"));

        Set<UUID> ids = encontradas.stream().map(Promotion::id).collect(Collectors.toSet());
        assertEquals(Set.of(primera, segunda), ids);
    }

    @Test
    void textoConComillasSeTrataComoUnIdentificadorMas() {
        assertTrue(adapter.findByProfessionalIds(List.of("x'); DROP TABLE promotions;--")).isEmpty());
    }

    @Test
    void listaVaciaNoConsultaLaBase() {
        assertTrue(adapter.findByProfessionalIds(List.of()).isEmpty());
    }
}
