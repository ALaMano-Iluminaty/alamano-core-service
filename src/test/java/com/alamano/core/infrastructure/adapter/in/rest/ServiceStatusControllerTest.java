package com.alamano.core.infrastructure.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceStatusControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createAcceptsOptionalDestinationAndPersistsIt() throws Exception {
        MvcResult withDestination = mockMvc.perform(post("/api/services")
                        .with(jwt().jwt(token -> token.subject("client-destination").claim("role", "CLIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"professionalId":"pro-dest","clientId":"client-destination",
                                 "destinationLatitude":4.65,"destinationLongitude":-74.06}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String destinationId = withDestination.getResponse().getContentAsString()
                .replaceAll(".*\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");

        MvcResult withoutDestination = mockMvc.perform(post("/api/services")
                        .with(jwt().jwt(token -> token.subject("client-no-destination").claim("role", "CLIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"professionalId":"pro-no-dest","clientId":"client-no-destination"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String noDestinationId = withoutDestination.getResponse().getContentAsString()
                .replaceAll(".*\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");

        assertEquals(4.65, jdbc.queryForObject("SELECT destination_latitude FROM services WHERE id = ?",
                Double.class, java.util.UUID.fromString(destinationId)));
        assertEquals(-74.06, jdbc.queryForObject("SELECT destination_longitude FROM services WHERE id = ?",
                Double.class, java.util.UUID.fromString(destinationId)));
        assertNull(jdbc.queryForObject("SELECT destination_latitude FROM services WHERE id = ?",
                Double.class, java.util.UUID.fromString(noDestinationId)));
    }

    @Test
    void invalidTransitionReturns409() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/services")
                        .with(jwt().jwt(token -> token.subject("client-1").claim("role", "CLIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"professionalId":"pro-1","clientId":"cli-1"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String body = created.getResponse().getContentAsString();
        String serviceId = body.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(patch("/api/services/" + serviceId + "/status")
                        .with(jwt().jwt(token -> token.subject("client-1").claim("role", "CLIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"COMPLETED"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("invalid_transition"));
    }

    @Test
    void validTransitionUpdatesStatus() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/services")
                        .with(jwt().jwt(token -> token.subject("client-2").claim("role", "CLIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"professionalId":"pro-2","clientId":"cli-2"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andReturn();

        String serviceId = created.getResponse().getContentAsString().replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(patch("/api/services/" + serviceId + "/status")
                        .with(jwt().jwt(token -> token.subject("client-2").claim("role", "CLIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"EN_ROUTE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_ROUTE"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.allowedTransitions").isArray());
    }
}
