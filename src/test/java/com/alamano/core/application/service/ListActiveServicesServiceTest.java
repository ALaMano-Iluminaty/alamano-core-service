package com.alamano.core.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListActiveServicesServiceTest {
    private static final Instant BASE = Instant.parse("2026-10-11T12:00:00Z");

    private ServiceRepositoryPort repository;
    private ListActiveServicesService useCase;

    @BeforeEach
    void setUp() {
        repository = mock(ServiceRepositoryPort.class);
        useCase = new ListActiveServicesService(repository);
        when(repository.findActiveByProfessionalId("user-1")).thenReturn(List.of());
        when(repository.findActiveByClientId("user-1")).thenReturn(List.of());
    }

    @Test
    void returnsEmptyListWhenUserHasNoActiveServices() {
        assertTrue(useCase.listActive("user-1").isEmpty());
    }

    @Test
    void returnsServicesWhereUserIsProfessional() {
        Service asProfessional = service("user-1", "client-9", 10);
        when(repository.findActiveByProfessionalId("user-1")).thenReturn(List.of(asProfessional));

        assertEquals(List.of(asProfessional), useCase.listActive("user-1"));
    }

    @Test
    void returnsServicesWhereUserIsClient() {
        Service asClient = service("pro-9", "user-1", 10);
        when(repository.findActiveByClientId("user-1")).thenReturn(List.of(asClient));

        assertEquals(List.of(asClient), useCase.listActive("user-1"));
    }

    @Test
    void mergesBothRolesNewestFirstWithoutDuplicates() {
        Service older = service("user-1", "client-9", 10);
        Service newer = service("pro-9", "user-1", 30);
        Service middle = service("user-1", "user-1", 20);
        when(repository.findActiveByProfessionalId("user-1")).thenReturn(List.of(middle, older));
        when(repository.findActiveByClientId("user-1")).thenReturn(List.of(newer, middle));

        assertEquals(List.of(newer, middle, older), useCase.listActive("user-1"));
    }

    private static Service service(String professionalId, String clientId, long secondsAfterBase) {
        Instant updatedAt = BASE.plusSeconds(secondsAfterBase);
        return new Service(UUID.randomUUID(), professionalId, clientId, ServiceStatus.EN_ROUTE, 1, BASE, updatedAt);
    }
}
