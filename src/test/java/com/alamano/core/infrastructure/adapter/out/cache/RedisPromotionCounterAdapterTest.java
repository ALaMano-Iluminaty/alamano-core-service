package com.alamano.core.infrastructure.adapter.out.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alamano.core.domain.promotion.PromotionCounterUnavailableException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * QA (AB#357): el adaptador que habla con Redis. El perfil 'test' usa el contador en
 * memoria, asi que sin estas pruebas este adaptador no se ejercitaba nunca, y la clave
 * que usa es un contrato: HU11 decrementa sobre esa misma clave.
 */
class RedisPromotionCounterAdapterTest {
    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private ValueOperations<String, String> operaciones;
    private RedisPromotionCounterAdapter adaptador;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        operaciones = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operaciones);
        adaptador = new RedisPromotionCounterAdapter(redis);
    }

    @Test
    void laClaveEsLaQueEsperaHU11() {
        assertEquals("promotion:11111111-2222-3333-4444-555555555555:cupos",
                RedisPromotionCounterAdapter.key(ID));
    }

    @Test
    void iniciarDejaElContadorEnElNumeroDeCupos() {
        adaptador.initialize(ID, 50);

        verify(operaciones).set(RedisPromotionCounterAdapter.key(ID), "50");
    }

    @Test
    void leerDevuelveLosCuposQueQuedan() {
        when(operaciones.get(RedisPromotionCounterAdapter.key(ID))).thenReturn("37");

        assertEquals(37, adaptador.remaining(ID).getAsInt());
    }

    @Test
    void leerDevuelveVacioSiLaClaveNoExiste() {
        when(operaciones.get(anyString())).thenReturn(null);

        assertTrue(adaptador.remaining(ID).isEmpty());
    }

    @Test
    void leerCeroEsUnValorValidoYNoVacio() {
        // Promocion agotada: son cero cupos, no "no hay contador".
        when(operaciones.get(anyString())).thenReturn("0");

        assertFalse(adaptador.remaining(ID).isEmpty());
        assertEquals(0, adaptador.remaining(ID).getAsInt());
    }

    @Test
    void siRedisNoRespondeAlIniciarSeAvisaConLaExcepcionDelDominio() {
        doThrow(new RedisConnectionFailureException("redis caido"))
                .when(operaciones).set(anyString(), anyString());

        PromotionCounterUnavailableException e = assertThrows(
                PromotionCounterUnavailableException.class, () -> adaptador.initialize(ID, 50));
        assertEquals(ID, e.promotionId());
        assertInstanceOf(RedisConnectionFailureException.class, e.getCause());
    }

    @Test
    void siRedisNoRespondeAlLeerSeAvisaConLaExcepcionDelDominio() {
        when(operaciones.get(anyString())).thenThrow(new RedisConnectionFailureException("redis caido"));

        assertThrows(PromotionCounterUnavailableException.class, () -> adaptador.remaining(ID));
    }

    @Test
    void tryClaimUsaElScriptYDevuelveCuposRestantes() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(3L);
        adaptador = new RedisPromotionCounterAdapter(redis);

        assertEquals(3, adaptador.tryClaim(ID).getAsInt());
    }

    @Test
    void tryClaimVacioSiNoQuedanCupos() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(-1L);
        adaptador = new RedisPromotionCounterAdapter(redis);

        assertTrue(adaptador.tryClaim(ID).isEmpty());
    }

    @Test
    void unValorCorruptoNoSePropagaComoErrorDeNumero() {
        // Si alguien escribe basura en la clave, debe salir el error del dominio,
        // no un NumberFormatException suelto que acabe en un 500.
        when(operaciones.get(anyString())).thenReturn("no-es-un-numero");

        assertThrows(PromotionCounterUnavailableException.class, () -> adaptador.remaining(ID));
    }
}
