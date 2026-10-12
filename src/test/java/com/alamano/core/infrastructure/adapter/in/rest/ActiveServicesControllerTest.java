package com.alamano.core.infrastructure.adapter.in.rest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.application.port.out.TrackingRepositoryPort;
import com.alamano.core.domain.professional.GeoPoint;
import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
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
class ActiveServicesControllerTest {
    private static final String ACTIVE_URL = "/api/services/active";
    private static final Instant BASE = Instant.parse("2026-10-11T12:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ServiceRepositoryPort services;
    @Autowired private TrackingRepositoryPort tracking;
    @Autowired private JwtAuthenticationConverter jwtAuthenticationConverter;

    private UUID proOneReserved;
    private UUID proOneInProgress;
    private UUID proTwoEnRoute;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM services");
        jdbc.update("DELETE FROM professionals");
        jdbc.update("INSERT INTO professionals (id, status, latitude, longitude) VALUES ('pro-1', 'BUSY', 4.65, -74.06)");
        proOneReserved = save("pro-1", "client-a", ServiceStatus.RESERVED, 10, 4.64, -74.05);
        proOneInProgress = save("pro-1", "client-b", ServiceStatus.IN_PROGRESS, 30, null, null);
        proTwoEnRoute = save("pro-2", "client-a", ServiceStatus.EN_ROUTE, 20, null, null);
        save("pro-1", "client-a", ServiceStatus.COMPLETED, 40, null, null);
        save("pro-2", "client-b", ServiceStatus.CANCELLED, 50, null, null);
    }

    @Test
    void professionalSeesOnlyOwnActiveServicesNewestFirstWithFullShape() throws Exception {
        tracking.saveLastLocationIfNewer(proOneReserved.toString(), new GeoPoint(4.65, -74.06), BASE.plusSeconds(15));

        mockMvc.perform(get(ACTIVE_URL).with(token("pro-1", "PROFESSIONAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(proOneInProgress.toString()))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].allowedTransitions[0]").value("COMPLETED"))
                .andExpect(jsonPath("$[1].id").value(proOneReserved.toString()))
                .andExpect(jsonPath("$[1].professionalId").value("pro-1"))
                .andExpect(jsonPath("$[1].clientId").value("client-a"))
                .andExpect(jsonPath("$[1].status").value("RESERVED"))
                .andExpect(jsonPath("$[1].version").value(0))
                .andExpect(jsonPath("$[1].allowedTransitions", containsInAnyOrder("EN_ROUTE", "CANCELLED")))
                .andExpect(jsonPath("$[1].destinationLatitude").value(4.64))
                .andExpect(jsonPath("$[1].destinationLongitude").value(-74.05))
                .andExpect(jsonPath("$[1].lastLatitude").value(4.65))
                .andExpect(jsonPath("$[1].lastLongitude").value(-74.06))
                .andExpect(jsonPath("$[1].etaSeconds").isNumber())
                .andExpect(jsonPath("$[1].professional.id").value("pro-1"))
                .andExpect(jsonPath("$[1].professional.status").value("BUSY"));
    }

    @Test
    void clientSeesOnlyOwnActiveServices() throws Exception {
        mockMvc.perform(get(ACTIVE_URL).with(token("client-a", "CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(proTwoEnRoute.toString()))
                .andExpect(jsonPath("$[1].id").value(proOneReserved.toString()));

        mockMvc.perform(get(ACTIVE_URL).with(token("client-b", "CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(proOneInProgress.toString()));
    }

    @Test
    void userWithoutActiveServicesGetsEmptyListNot404() throws Exception {
        mockMvc.perform(get(ACTIVE_URL).with(token("stranger", "CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get(ACTIVE_URL).with(token("pro-3", "PROFESSIONAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get(ACTIVE_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void serviceByIdRouteStillWorks() throws Exception {
        mockMvc.perform(get("/api/services/" + proOneReserved).with(token("client-a", "CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(proOneReserved.toString()));
    }

    private UUID save(String professionalId, String clientId, ServiceStatus status, long secondsAfterBase,
            Double destinationLatitude, Double destinationLongitude) {
        UUID id = UUID.randomUUID();
        services.save(new Service(id, professionalId, clientId, status, 0, BASE, BASE.plusSeconds(secondsAfterBase),
                destinationLatitude, destinationLongitude));
        return id;
    }

    private RequestPostProcessor token(String subject, String role) {
        return jwt().jwt(token -> token.subject(subject).claim("role", role))
                .authorities(jwt -> new ArrayList<>(jwtAuthenticationConverter.convert(jwt).getAuthorities()));
    }
}
