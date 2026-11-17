package com.alamano.core.infrastructure.adapter.out.cache;

import com.alamano.core.application.port.out.ProfessionalQueryPort;
import com.alamano.core.domain.professional.GeoPoint;
import com.alamano.core.domain.professional.NearbyProfessional;
import com.alamano.core.domain.professional.SearchArea;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * HU-Q9: cache en Redis de la consulta de barberos cercanos.
 *
 * Muchos clientes miran el mismo mapa a la vez; en vez de calcular la distancia en
 * Postgres para cada uno, la consulta se hace una vez por celda de la cuadricula
 * (~1,1 km de lado) y se comparte entre todas las instancias del Core mientras dure
 * la vigencia (alamano.nearby.cache-ttl-seconds, 10 s por defecto). El mapa en vivo
 * sigue llegando por WebSocket, asi que la vigencia corta solo retrasa la lista inicial.
 *
 * Contrato: la distancia que devuelve es la del centro de la celda, no la del punto
 * exacto del cliente (diferencia maxima ~0,8 km). Si Redis falla, la consulta cae a la
 * base: la cache es una optimizacion y nunca debe tumbar el servicio (HU-Q1).
 */
@Component
@Primary
@Profile("!test")
public class RedisCachedProfessionalQuery implements ProfessionalQueryPort {
    private static final Logger log = LoggerFactory.getLogger(RedisCachedProfessionalQuery.class);
    private static final TypeReference<List<NearbyProfessional>> LISTA = new TypeReference<>() {};

    /** Lado de la celda en grados: 0,01 grados son ~1,1 km. */
    static final double CELDA_GRADOS = 0.01;

    private final ProfessionalQueryPort baseDeDatos;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Duration vigencia;
    private final Counter aciertos;
    private final Counter fallos;

    public RedisCachedProfessionalQuery(
            @Qualifier("professionalJdbcAdapter") ProfessionalQueryPort baseDeDatos,
            StringRedisTemplate redis,
            ObjectMapper mapper,
            MeterRegistry registro,
            @Value("${alamano.nearby.cache-ttl-seconds:10}") long vigenciaSegundos) {
        this.baseDeDatos = baseDeDatos;
        this.redis = redis;
        this.mapper = mapper;
        this.vigencia = Duration.ofSeconds(vigenciaSegundos);
        this.aciertos = Counter.builder("alamano.nearby.cache").tag("result", "hit").register(registro);
        this.fallos = Counter.builder("alamano.nearby.cache").tag("result", "miss").register(registro);
    }

    @Override
    public List<NearbyProfessional> findAvailableWithin(SearchArea area, int limit) {
        double latitud = acotar(ajustar(area.center().latitude()), 90);
        double longitud = acotar(ajustar(area.center().longitude()), 180);
        String clave = String.format(Locale.ROOT, "nearby:%.2f:%.2f:%.1f:%d", latitud, longitud, area.radiusKm(), limit);

        List<NearbyProfessional> guardada = leer(clave);
        if (guardada != null) {
            aciertos.increment();
            return guardada;
        }
        fallos.increment();
        SearchArea celda = new SearchArea(new GeoPoint(latitud, longitud), area.radiusKm());
        List<NearbyProfessional> recien = baseDeDatos.findAvailableWithin(celda, limit);
        guardar(clave, recien);
        return recien;
    }

    private static double ajustar(double grados) {
        return Math.round(grados / CELDA_GRADOS) * CELDA_GRADOS;
    }

    private static double acotar(double valor, double maximo) {
        return Math.max(-maximo, Math.min(maximo, valor));
    }

    private List<NearbyProfessional> leer(String clave) {
        try {
            String json = redis.opsForValue().get(clave);
            return json == null ? null : mapper.readValue(json, LISTA);
        } catch (JsonProcessingException | DataAccessException e) {
            log.warn("Cache de cercanos no disponible para {}: {}", clave, e.getMessage());
            return null;
        }
    }

    private void guardar(String clave, List<NearbyProfessional> lista) {
        try {
            redis.opsForValue().set(clave, mapper.writeValueAsString(lista), vigencia);
        } catch (JsonProcessingException | DataAccessException e) {
            log.warn("No se pudo guardar la cache de cercanos {}: {}", clave, e.getMessage());
        }
    }
}
