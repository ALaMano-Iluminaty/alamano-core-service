package com.alamano.core.infrastructure.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ServiceActiveJdbcAdapterTest {
    private static final Instant BASE = Instant.parse("2026-10-11T12:00:00Z");

    @Autowired JdbcTemplate jdbc;
    @Autowired ServiceRepositoryPort services;

    @BeforeEach
    void clearServices() { jdbc.update("DELETE FROM services"); }

    @Test
    void professionalGetsOnlyOwnNonTerminalServicesNewestFirst() {
        UUID reserved = save("pro-1", "client-1", ServiceStatus.RESERVED, 10);
        UUID inProgress = save("pro-1", "client-2", ServiceStatus.IN_PROGRESS, 30);
        UUID arrived = save("pro-1", "client-3", ServiceStatus.ARRIVED, 20);
        save("pro-1", "client-4", ServiceStatus.COMPLETED, 40);
        save("pro-1", "client-5", ServiceStatus.CANCELLED, 50);
        save("pro-2", "client-1", ServiceStatus.EN_ROUTE, 60);

        assertEquals(List.of(inProgress, arrived, reserved), ids(services.findActiveByProfessionalId("pro-1")));
    }

    @Test
    void clientGetsOnlyOwnNonTerminalServicesNewestFirst() {
        UUID enRoute = save("pro-1", "client-1", ServiceStatus.EN_ROUTE, 10);
        UUID reserved = save("pro-2", "client-1", ServiceStatus.RESERVED, 20);
        save("pro-3", "client-1", ServiceStatus.COMPLETED, 30);
        save("pro-1", "client-2", ServiceStatus.IN_PROGRESS, 40);

        assertEquals(List.of(reserved, enRoute), ids(services.findActiveByClientId("client-1")));
    }

    @Test
    void returnsEmptyListsWhenNothingIsActive() {
        save("pro-1", "client-1", ServiceStatus.COMPLETED, 10);

        assertTrue(services.findActiveByProfessionalId("pro-1").isEmpty());
        assertTrue(services.findActiveByClientId("client-1").isEmpty());
        assertTrue(services.findActiveByClientId("nobody").isEmpty());
    }

    private UUID save(String professionalId, String clientId, ServiceStatus status, long secondsAfterBase) {
        UUID id = UUID.randomUUID();
        services.save(new Service(id, professionalId, clientId, status, 0, BASE, BASE.plusSeconds(secondsAfterBase)));
        return id;
    }

    private static List<UUID> ids(List<Service> found) {
        return found.stream().map(Service::id).toList();
    }
}
