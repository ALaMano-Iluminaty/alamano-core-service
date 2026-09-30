package com.alamano.core.infrastructure.adapter.in.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfessionalControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM professionals");
        jdbc.update(
                "INSERT INTO professionals (id, status, latitude, longitude) VALUES (?, ?, ?, ?)",
                "cerca-1",
                "AVAILABLE",
                4.6576,
                -74.0628);
        jdbc.update(
                "INSERT INTO professionals (id, status, latitude, longitude) VALUES (?, ?, ?, ?)",
                "cerca-2",
                "AVAILABLE",
                4.6756,
                -74.0628);
    }

    @Test
    void returnsNearbyProfessionalsForAuthenticatedClient() throws Exception {
        mockMvc.perform(authenticated(get("/api/professionals/nearby")
                        .param("lat", "4.6486")
                        .param("lng", "-74.0628")
                        .param("radiusKm", "5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].professionalId").value("cerca-1"))
                .andExpect(jsonPath("$[0].distanceKm").value(1.0))
                .andExpect(jsonPath("$[1].professionalId").value("cerca-2"))
                .andExpect(jsonPath("$[1].distanceKm").value(3.0));
    }

    @Test
    void usesDefaultRadiusWhenRadiusIsOmitted() throws Exception {
        mockMvc.perform(authenticated(get("/api/professionals/nearby")
                        .param("lat", "4.6486")
                        .param("lng", "-74.0628")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].professionalId").value("cerca-1"))
                .andExpect(jsonPath("$[1].professionalId").value("cerca-2"));
    }

    @Test
    void rejectsRadiusAboveMaximum() throws Exception {
        mockMvc.perform(authenticated(get("/api/professionals/nearby")
                        .param("lat", "4.6486")
                        .param("lng", "-74.0628")
                        .param("radiusKm", "50")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_search_area"));
    }

    @Test
    void rejectsLatitudeOutsideRange() throws Exception {
        mockMvc.perform(authenticated(get("/api/professionals/nearby")
                        .param("lat", "100")
                        .param("lng", "-74.0628")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_search_area"));
    }

    @Test
    void reportsMissingRequiredParameterAsInvalidRequest() throws Exception {
        mockMvc.perform(authenticated(get("/api/professionals/nearby").param("lng", "-74.0628")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/professionals/nearby")
                        .param("lat", "4.6486")
                        .param("lng", "-74.0628"))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.with(jwt().jwt(token -> token.subject("client-1").claim("role", "CLIENT")));
    }
}
