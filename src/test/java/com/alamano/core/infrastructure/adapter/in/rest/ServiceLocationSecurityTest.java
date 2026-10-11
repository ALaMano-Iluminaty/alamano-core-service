package com.alamano.core.infrastructure.adapter.in.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * [QA AB#340] La ubicación solo se acepta del vendedor dueño del servicio y solo mientras
 * el servicio sigue activo.
 *
 * <p>La regla ya está probada por dentro en {@code UpdateTrackingServiceTest}, pero por otro
 * camino: ese cubre las ubicaciones que llegan por eventos, que se descartan en silencio.
 * El camino REST es más estricto y responde con un código distinto para cada motivo, y eso
 * es justo lo que no estaba probado: {@code MvpCoreApiTest} solo recorre el camino feliz.
 *
 * <p>Además de los códigos, al final se comprueba que ninguno de los intentos rechazados
 * alcanzó a guardar su ubicación. Por eso el dueño reporta de primero y los demás intentan
 * pisarlo después: el Core solo conserva la última ubicación, así que si alguno de esos
 * rechazos fallara, la suya quedaría encima y la comprobación final se caería. Al revés
 * —rechazos primero— la prueba pasaría igual aunque el Core aceptara todo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceLocationSecurityTest {
    private static final String DUENO = "barber-dueno";
    private static final String INTRUSO = "barber-intruso";
    private static final String CLIENTE = "client-a";

    /** La ubicación buena, la del vendedor dueño. */
    private static final double LAT_OK = 4.65;
    /** La que intentan colar los demás. Nunca debe quedar guardada. */
    private static final double LAT_INTRUSA = 9.99;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM promotion_claims");
        jdbc.update("DELETE FROM promotions");
        jdbc.update("DELETE FROM services");
        jdbc.update("DELETE FROM professionals");
        jdbc.update(
                "INSERT INTO professionals (id, status, latitude, longitude) VALUES (?, 'AVAILABLE', 4.6576, -74.0628)",
                DUENO);
        jdbc.update(
                "INSERT INTO professionals (id, status, latitude, longitude) VALUES (?, 'AVAILABLE', 4.6580, -74.0630)",
                INTRUSO);
    }

    @Test
    void soloElVendedorDelServicioPuedeReportarUbicacion() throws Exception {
        String servicio = reservar();

        // Control: el camino bueno funciona. Sin esto, un "no se guardó nada" podría ser
        // simplemente que la ubicación nunca se guarda, por cualquier otra razón.
        reportar(servicio, DUENO, "PROFESSIONAL", LAT_OK).andExpect(status().isNoContent());
        servicioVistoPorElCliente(servicio).andExpect(jsonPath("$.lastLatitude").value(LAT_OK));

        // Un cliente no llega ni al caso de uso: lo corta la regla de seguridad del rol.
        reportar(servicio, CLIENTE, "CLIENT", LAT_INTRUSA).andExpect(status().isForbidden());

        // Otro vendedor sí tiene el rol, pero el servicio no es suyo.
        reportar(servicio, INTRUSO, "PROFESSIONAL", LAT_INTRUSA).andExpect(status().isForbidden());

        // Un servicio que no existe no puede reventar el endpoint ni responder 204.
        reportar(UUID.randomUUID().toString(), INTRUSO, "PROFESSIONAL", LAT_INTRUSA)
                .andExpect(status().isNotFound());

        // Ninguno de los rechazos alcanzó a pisar la ubicación del dueño.
        servicioVistoPorElCliente(servicio).andExpect(jsonPath("$.lastLatitude").value(LAT_OK));
    }

    @Test
    void sinServicioActivoNoSeAceptaUbicacion() throws Exception {
        String servicio = reservar();
        reportar(servicio, DUENO, "PROFESSIONAL", LAT_OK).andExpect(status().isNoContent());

        // El cliente cancela: el servicio queda en un estado terminal.
        mockMvc.perform(patch("/api/services/" + servicio + "/status")
                        .with(token(CLIENTE, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Desde aquí ya no hay servicio en curso: ni el propio dueño puede seguir
        // reportando. 409, no 204: el Core lo rechaza, no lo ignora.
        reportar(servicio, DUENO, "PROFESSIONAL", LAT_INTRUSA).andExpect(status().isConflict());

        servicioVistoPorElCliente(servicio).andExpect(jsonPath("$.lastLatitude").value(LAT_OK));
    }

    /** El cliente reserva al vendedor dueño y devuelve el id del servicio creado. */
    private String reservar() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/services")
                        .with(token(CLIENTE, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"professionalId":"%s","destinationLatitude":4.64,"destinationLongitude":-74.06}
                                """.formatted(DUENO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(cuerpo, "$.id");
    }

    private ResultActions reportar(String servicio, String sujeto, String rol, double latitud) throws Exception {
        return mockMvc.perform(post("/api/services/" + servicio + "/location")
                .with(token(sujeto, rol))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lat\":%s,\"lng\":-74.06}".formatted(latitud)));
    }

    private ResultActions servicioVistoPorElCliente(String servicio) throws Exception {
        return mockMvc.perform(get("/api/services/" + servicio).with(token(CLIENTE, "CLIENT")))
                .andExpect(status().isOk());
    }

    private RequestPostProcessor token(String subject, String role) {
        return jwt().jwt(token -> token.subject(subject).claim("role", role))
                .authorities(jwt -> new ArrayList<>(jwtAuthenticationConverter.convert(jwt).getAuthorities()));
    }
}
