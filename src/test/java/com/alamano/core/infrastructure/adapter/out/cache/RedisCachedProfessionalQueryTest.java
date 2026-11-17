package com.alamano.core.infrastructure.adapter.out.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alamano.core.application.port.out.ProfessionalQueryPort;
import com.alamano.core.domain.professional.GeoPoint;
import com.alamano.core.domain.professional.NearbyProfessional;
import com.alamano.core.domain.professional.SearchArea;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** HU-Q9: la cache de barberos cercanos acierta, cae a la base y nunca tumba la consulta. */
class RedisCachedProfessionalQueryTest {
    private static final List<NearbyProfessional> LISTA =
            List.of(new NearbyProfessional("barbero-1", new GeoPoint(4.66, -74.06), 0.4));

    private ProfessionalQueryPort baseDeDatos;
    private ValueOperations<String, String> operaciones;
    private SimpleMeterRegistry registro;
    private ObjectMapper mapper;
    private RedisCachedProfessionalQuery cache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        baseDeDatos = mock(ProfessionalQueryPort.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        operaciones = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operaciones);
        registro = new SimpleMeterRegistry();
        mapper = new ObjectMapper();
        cache = new RedisCachedProfessionalQuery(baseDeDatos, redis, mapper, registro, 10);
        when(baseDeDatos.findAvailableWithin(any(), anyInt())).thenReturn(LISTA);
    }

    private static SearchArea area(double latitud, double longitud) {
        return new SearchArea(new GeoPoint(latitud, longitud), 5.0);
    }

    private double contador(String resultado) {
        return registro.get("alamano.nearby.cache").tag("result", resultado).counter().count();
    }

    @Test
    void sinCacheConsultaLaBaseYGuardaElResultadoConVigencia() {
        when(operaciones.get(anyString())).thenReturn(null);

        List<NearbyProfessional> resultado = cache.findAvailableWithin(area(4.6576, -74.0628), 50);

        assertEquals(LISTA, resultado);
        ArgumentCaptor<SearchArea> consultada = ArgumentCaptor.forClass(SearchArea.class);
        verify(baseDeDatos).findAvailableWithin(consultada.capture(), eq(50));
        assertEquals(4.66, consultada.getValue().center().latitude(), 1e-9);
        assertEquals(-74.06, consultada.getValue().center().longitude(), 1e-9);
        verify(operaciones).set(eq("nearby:4.66:-74.06:5.0:50"), anyString(), eq(Duration.ofSeconds(10)));
        assertEquals(1, contador("miss"));
        assertEquals(0, contador("hit"));
    }

    @Test
    void conCacheNoTocaLaBase() throws Exception {
        when(operaciones.get("nearby:4.66:-74.06:5.0:50")).thenReturn(mapper.writeValueAsString(LISTA));

        List<NearbyProfessional> resultado = cache.findAvailableWithin(area(4.6576, -74.0628), 50);

        assertEquals(LISTA, resultado);
        verify(baseDeDatos, never()).findAvailableWithin(any(), anyInt());
        assertEquals(1, contador("hit"));
        assertEquals(0, contador("miss"));
    }

    @Test
    void dosPuntosDeLaMismaCeldaComparteLaClave() {
        when(operaciones.get(anyString())).thenReturn(null);

        cache.findAvailableWithin(area(4.6576, -74.0628), 50);
        cache.findAvailableWithin(area(4.6601, -74.0599), 50);

        ArgumentCaptor<String> claves = ArgumentCaptor.forClass(String.class);
        verify(operaciones, times(2)).get(claves.capture());
        assertEquals(claves.getAllValues().get(0), claves.getAllValues().get(1));
    }

    @Test
    void celdasDistintasUsanClavesDistintas() {
        when(operaciones.get(anyString())).thenReturn(null);

        cache.findAvailableWithin(area(4.6576, -74.0628), 50);
        cache.findAvailableWithin(area(4.7500, -74.0628), 50);

        ArgumentCaptor<String> claves = ArgumentCaptor.forClass(String.class);
        verify(operaciones, times(2)).get(claves.capture());
        assertNotEquals(claves.getAllValues().get(0), claves.getAllValues().get(1));
    }

    @Test
    void unaListaVaciaTambienSeGuarda() {
        when(operaciones.get(anyString())).thenReturn(null);
        when(baseDeDatos.findAvailableWithin(any(), anyInt())).thenReturn(List.of());

        assertTrue(cache.findAvailableWithin(area(4.6576, -74.0628), 50).isEmpty());

        verify(operaciones).set(anyString(), eq("[]"), any(Duration.class));
    }

    @Test
    void siRedisFallaAlLeerRespondeDesdeLaBase() {
        when(operaciones.get(anyString())).thenThrow(new RedisConnectionFailureException("caido"));

        assertEquals(LISTA, cache.findAvailableWithin(area(4.6576, -74.0628), 50));
        assertEquals(1, contador("miss"));
    }

    @Test
    void siRedisFallaAlGuardarIgualResponde() {
        when(operaciones.get(anyString())).thenReturn(null);
        doThrow(new RedisConnectionFailureException("caido"))
                .when(operaciones).set(anyString(), anyString(), any(Duration.class));

        assertEquals(LISTA, cache.findAvailableWithin(area(4.6576, -74.0628), 50));
    }

    @Test
    void unaCacheCorruptaSeIgnoraYSeVuelveAConsultar() {
        when(operaciones.get(anyString())).thenReturn("esto no es json");

        assertEquals(LISTA, cache.findAvailableWithin(area(4.6576, -74.0628), 50));
        verify(baseDeDatos).findAvailableWithin(any(), anyInt());
    }

    @Test
    void enLosExtremosDelGloboLaCeldaNoSeSaleDeRango() {
        when(operaciones.get(anyString())).thenReturn(null);

        cache.findAvailableWithin(area(90, 180), 50);
        cache.findAvailableWithin(area(-90, -180), 50);

        verify(baseDeDatos, times(2)).findAvailableWithin(any(), anyInt());
    }
}
