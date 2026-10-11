package com.alamano.core.infrastructure.adapter.in.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MvpCoreApiTest {
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
                "INSERT INTO professionals (id, status, latitude, longitude) VALUES ('barber-mvp', 'AVAILABLE', 4.6576, -74.0628)");
    }

    @Test
    void reservationTakesClientIdFromTokenAndSecondClientGets409() throws Exception {
        String body = mockMvc.perform(post("/api/services")
                        .with(token("client-a", "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"professionalId":"barber-mvp","clientId":"should-be-ignored",
                                 "destinationLatitude":4.64,"destinationLongitude":-74.06}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value("client-a"))
                .andExpect(jsonPath("$.destinationLatitude").value(4.64))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String serviceId = JsonPath.read(body, "$.id");

        mockMvc.perform(post("/api/services")
                        .with(token("client-b", "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionalId\":\"barber-mvp\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("professional_busy"));

        mockMvc.perform(get("/api/services/" + serviceId).with(token("client-a", "CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serviceId))
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.professional.id").value("barber-mvp"));

        mockMvc.perform(get("/api/services/" + serviceId).with(token("stranger", "CLIENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void claimIsAtomicAndRejectsWhenSlotsAreGoneOrRepeated() throws Exception {
        String created = mockMvc.perform(post("/api/promotions")
                        .with(token("barber-mvp", "PROFESSIONAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Corte 2x1\",\"totalSlots\":1}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String promoId = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/professionals/nearby")
                        .with(token("client-a", "CLIENT"))
                        .param("lat", "4.6486")
                        .param("lng", "-74.0628")
                        .param("radiusKm", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].promotions[0].id").value(promoId))
                .andExpect(jsonPath("$[0].promotions[0].availableSlots").value(1));

        mockMvc.perform(get("/api/professionals/barber-mvp/promotions").with(token("client-a", "CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(promoId));

        mockMvc.perform(get("/api/promotions/mine").with(token("barber-mvp", "PROFESSIONAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(promoId));

        mockMvc.perform(post("/api/promotions/" + promoId + "/claims").with(token("client-a", "CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remaining").value(0))
                .andExpect(jsonPath("$.total").value(1));

        mockMvc.perform(post("/api/promotions/" + promoId + "/claim").with(token("client-b", "CLIENT")))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/promotions/" + promoId + "/claims").with(token("client-a", "CLIENT")))
                .andExpect(status().isConflict());
    }

    @Test
    void professionalCanReportLocationOnActiveService() throws Exception {
        String body = mockMvc.perform(post("/api/services")
                        .with(token("client-a", "CLIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"professionalId":"barber-mvp",
                                 "destinationLatitude":4.64,"destinationLongitude":-74.06}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String serviceId = JsonPath.read(body, "$.id");

        mockMvc.perform(post("/api/services/" + serviceId + "/location")
                        .with(token("barber-mvp", "PROFESSIONAL"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lat\":4.65,\"lng\":-74.06}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/services/" + serviceId).with(token("client-a", "CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastLatitude").value(4.65))
                .andExpect(jsonPath("$.etaSeconds").exists());
    }

    private RequestPostProcessor token(String subject, String role) {
        return jwt().jwt(token -> token.subject(subject).claim("role", role))
                .authorities(jwt -> new ArrayList<>(jwtAuthenticationConverter.convert(jwt).getAuthorities()));
    }
}
