package com.alamano.core.infrastructure.adapter.in.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DisconnectProfessionalControllerTest {
    private static final String OFFLINE_URL = "/api/professionals/me/offline";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtAuthenticationConverter jwtAuthenticationConverter;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM professionals");
    }

    @Test
    void disconnectsAndProfessionalDisappearsFromNearbySearch() throws Exception {
        mockMvc.perform(post("/api/professionals/me/online").with(token("pro-1", "PROFESSIONAL"))
                        .contentType("application/json").content("{\"latitude\":4.6486,\"longitude\":-74.0628}"))
                .andExpect(status().isOk());

        mockMvc.perform(post(OFFLINE_URL).with(token("pro-1", "PROFESSIONAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OFFLINE"));

        mockMvc.perform(get("/api/professionals/nearby").with(token("client-1", "CLIENT"))
                        .param("lat", "4.6486").param("lng", "-74.0628"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void alreadyOfflineRequestReturnsOk() throws Exception {
        jdbc.update("INSERT INTO professionals (id, status, latitude, longitude) VALUES (?, ?, ?, ?)",
                "pro-1", "OFFLINE", 4.6486, -74.0628);
        mockMvc.perform(post(OFFLINE_URL).with(token("pro-1", "PROFESSIONAL")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OFFLINE"));
    }

    @Test
    void busyProfessionalReturnsConflict() throws Exception {
        jdbc.update("INSERT INTO professionals (id, status, latitude, longitude) VALUES (?, ?, ?, ?)",
                "pro-1", "BUSY", 4.6486, -74.0628);
        mockMvc.perform(post(OFFLINE_URL).with(token("pro-1", "PROFESSIONAL")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("professional_busy"));
    }

    @Test
    void unknownProfessionalReturnsNotFound() throws Exception {
        mockMvc.perform(post(OFFLINE_URL).with(token("missing", "PROFESSIONAL")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("professional_not_found"))
                .andExpect(jsonPath("$.message").value("No existe un vendedor con ese id"));
    }

    @Test
    void clientRoleIsForbidden() throws Exception {
        mockMvc.perform(post(OFFLINE_URL).with(token("client-1", "CLIENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticationIsRequired() throws Exception {
        mockMvc.perform(post(OFFLINE_URL)).andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor token(String subject, String role) {
        return jwt().jwt(jwt -> jwt.subject(subject).claim("role", role))
                .authorities(jwt -> new ArrayList<>(jwtAuthenticationConverter.convert(jwt).getAuthorities()));
    }
}
