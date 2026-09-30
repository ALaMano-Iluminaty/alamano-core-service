package com.alamano.core.infrastructure.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Map;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConnectProfessionalControllerTest {
    private static final String ONLINE_URL = "/api/professionals/me/online";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM professionals");
    }

    @Test
    void professionalGoesOnlineAndIsStored() throws Exception {
        mockMvc.perform(online(token("pro-1", "PROFESSIONAL"), "{\"latitude\":4.6486,\"longitude\":-74.0628}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.professionalId").value("pro-1"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.latitude").value(4.6486))
                .andExpect(jsonPath("$.longitude").value(-74.0628))
                .andExpect(jsonPath("$.version").value(0));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, latitude, longitude FROM professionals WHERE id = ?", "pro-1");
        assertEquals("AVAILABLE", row.get("status"));
        assertEquals(4.6486, ((Number) row.get("latitude")).doubleValue());
        assertEquals(-74.0628, ((Number) row.get("longitude")).doubleValue());
    }

    @Test
    void connectedProfessionalAppearsInNearbySearch() throws Exception {
        mockMvc.perform(online(token("pro-1", "PROFESSIONAL"), "{\"latitude\":4.6576,\"longitude\":-74.0628}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/professionals/nearby")
                        .with(token("client-1", "CLIENT"))
                        .param("lat", "4.6486")
                        .param("lng", "-74.0628"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].professionalId").value("pro-1"));
    }

    @Test
    void clientCannotGoOnline() throws Exception {
        mockMvc.perform(online(token("client-1", "CLIENT"), "{\"latitude\":4.6486,\"longitude\":-74.0628}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post(ONLINE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":4.6486,\"longitude\":-74.0628}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingLatitudeIsInvalidRequest() throws Exception {
        mockMvc.perform(online(token("pro-1", "PROFESSIONAL"), "{\"longitude\":-74.0628}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    @Test
    void latitudeOutOfRangeIsRejected() throws Exception {
        mockMvc.perform(online(token("pro-1", "PROFESSIONAL"), "{\"latitude\":100,\"longitude\":-74.0628}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void busyProfessionalCannotGoOnline() throws Exception {
        jdbc.update(
                "INSERT INTO professionals (id, status, latitude, longitude) VALUES (?, ?, ?, ?)",
                "pro-1",
                "BUSY",
                4.6486,
                -74.0628);

        mockMvc.perform(online(token("pro-1", "PROFESSIONAL"), "{\"latitude\":4.6486,\"longitude\":-74.0628}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("professional_busy"));
    }

    private MockHttpServletRequestBuilder online(RequestPostProcessor token, String body) {
        return post(ONLINE_URL).with(token).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    /** Los permisos salen del convertidor real de SecurityConfig (claim role → ROLE_<role>). */
    private RequestPostProcessor token(String subject, String role) {
        return jwt().jwt(token -> token.subject(subject).claim("role", role))
                .authorities(jwt -> new ArrayList<>(jwtAuthenticationConverter.convert(jwt).getAuthorities()));
    }
}
