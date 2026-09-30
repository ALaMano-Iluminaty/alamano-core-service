package com.alamano.core.infrastructure.adapter.in.rest;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceStatusControllerTest {
    @Autowired
    private MockMvc mockMvc;

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
