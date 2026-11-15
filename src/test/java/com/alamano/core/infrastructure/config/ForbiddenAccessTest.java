package com.alamano.core.infrastructure.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
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
 * HU-Q3: pruebas automatizadas de accesos indebidos. Un cliente autenticado que intenta
 * publicar una promocion, ponerse en linea como barbero o tocar un servicio ajeno recibe 403,
 * y ningun intento deja rastro en la base.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ForbiddenAccessTest {
    private static final String BARBERO = "barber-dueno";
    private static final String OTRO_BARBERO = "barber-ajeno";
    private static final String CLIENTE = "client-a";
    private static final String OTRO_CLIENTE = "client-b";

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
        jdbc.update("INSERT INTO professionals (id, status, latitude, longitude) VALUES (?, 'AVAILABLE', 4.6576, -74.0628)", BARBERO);
        jdbc.update("INSERT INTO professionals (id, status, latitude, longitude) VALUES (?, 'AVAILABLE', 4.6580, -74.0630)", OTRO_BARBERO);
    }

    @Test
    void unClienteNoPuedePublicarPromociones() throws Exception {
        mockMvc.perform(post("/api/promotions").with(token(CLIENTE, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"2x1 intruso\",\"totalSlots\":5}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/promotions/mine").with(token(CLIENTE, "CLIENT")))
                .andExpect(status().isForbidden());

        assertNoHayPromociones();
    }

    @Test
    void unClienteNoPuedePonerseEnLineaComoBarbero() throws Exception {
        mockMvc.perform(post("/api/professionals/me/online").with(token(CLIENTE, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":4.6,\"longitude\":-74.0}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/professionals/me/offline").with(token(CLIENTE, "CLIENT")))
                .andExpect(status().isForbidden());

        Integer creados = jdbc.queryForObject("SELECT COUNT(*) FROM professionals WHERE id = ?", Integer.class, CLIENTE);
        org.junit.jupiter.api.Assertions.assertEquals(0, creados, "el cliente no debe quedar registrado como barbero");
    }

    @Test
    void unTokenSinRolNoPuedeHacerNadaReservadoAUnRol() throws Exception {
        mockMvc.perform(post("/api/promotions").with(token(CLIENTE, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"x\",\"totalSlots\":5}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/professionals/me/online").with(token(CLIENTE, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":4.6,\"longitude\":-74.0}"))
                .andExpect(status().isForbidden());

        assertNoHayPromociones();
    }

    @Test
    void nadieAjenoAlServicioPuedeVerloNiCambiarleElEstado() throws Exception {
        String servicio = reservar();

        // Otro cliente: ni lo ve ni lo cancela.
        mockMvc.perform(get("/api/services/" + servicio).with(token(OTRO_CLIENTE, "CLIENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("service_access_denied"));
        cambiarEstado(servicio, OTRO_CLIENTE, "CLIENT", "CANCELLED").andExpect(status().isForbidden());

        // Otro barbero: tiene el rol, pero el servicio no es suyo.
        cambiarEstado(servicio, OTRO_BARBERO, "PROFESSIONAL", "EN_ROUTE").andExpect(status().isForbidden());

        // El propio cliente solo puede cancelar, no avanzar el servicio.
        cambiarEstado(servicio, CLIENTE, "CLIENT", "EN_ROUTE").andExpect(status().isForbidden());

        // Ninguno de los rechazos movio el estado.
        mockMvc.perform(get("/api/services/" + servicio).with(token(CLIENTE, "CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    private String reservar() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/services").with(token(CLIENTE, "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionalId\":\"%s\",\"destinationLatitude\":4.64,\"destinationLongitude\":-74.06}"
                                .formatted(BARBERO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.id");
    }

    private ResultActions cambiarEstado(String servicio, String sujeto, String rol, String estado) throws Exception {
        return mockMvc.perform(patch("/api/services/" + servicio + "/status").with(token(sujeto, rol))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + estado + "\"}"));
    }

    private void assertNoHayPromociones() {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM promotions", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(0, total, "ningun acceso indebido debe dejar rastro");
    }

    private RequestPostProcessor token(String subject, String role) {
        return jwt().jwt(token -> {
                    token.subject(subject);
                    if (role != null) token.claim("role", role);
                })
                .authorities(jwt -> new ArrayList<>(jwtAuthenticationConverter.convert(jwt).getAuthorities()));
    }
}
