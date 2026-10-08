package com.alamano.core.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.application.port.out.ServiceStatusChangedPublisherPort;
import com.alamano.core.domain.service.InvalidServiceStatusTransitionException;
import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceAccessDeniedException;
import com.alamano.core.domain.service.ServiceStatus;
import com.alamano.core.domain.service.ServiceStatusConflictException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;

class ChangeServiceStatusServiceTest {
    private final UUID id = UUID.randomUUID();
    private ServiceRepositoryPort repository;
    private ServiceStatusChangedPublisherPort publisher;
    private ChangeServiceStatusService useCase;

    @BeforeEach
    void setUp() {
        repository = mock(ServiceRepositoryPort.class);
        publisher = mock(ServiceStatusChangedPublisherPort.class);
        useCase = new ChangeServiceStatusService(repository, publisher,
                Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC));
        when(repository.updateStatusIfMatches(any(), any(), any(Long.class), any())).thenReturn(true);
    }

    @Test
    void professionalCanAdvanceAndPublishes() {
        given(ServiceStatus.RESERVED);
        Service updated = useCase.changeStatus(id, ServiceStatus.EN_ROUTE, "pro-1", null);
        assertEquals(ServiceStatus.EN_ROUTE, updated.status());
        verify(publisher).publish(updated, ServiceStatus.RESERVED, null);
    }

    @Test
    void clientCanCancelFromReserved() {
        given(ServiceStatus.RESERVED);
        Service updated = useCase.changeStatus(id, ServiceStatus.CANCELLED, "cli-1", null);
        assertEquals(ServiceStatus.CANCELLED, updated.status());
        verify(publisher).publish(updated, ServiceStatus.RESERVED, null);
    }

    @Test
    void clientCannotAdvance() {
        given(ServiceStatus.RESERVED);
        assertThrows(ServiceAccessDeniedException.class,
                () -> useCase.changeStatus(id, ServiceStatus.EN_ROUTE, "cli-1", null));
        verify(publisher, never()).publish(any(), any(), any());
    }

    @Test
    void thirdPartyCannotChange() {
        given(ServiceStatus.RESERVED);
        assertThrows(ServiceAccessDeniedException.class,
                () -> useCase.changeStatus(id, ServiceStatus.EN_ROUTE, "other", null));
        verify(publisher, never()).publish(any(), any(), any());
    }

    @Test
    void professionalIllegalTransitionRemainsConflictWithoutPublishing() {
        given(ServiceStatus.RESERVED);
        assertThrows(InvalidServiceStatusTransitionException.class,
                () -> useCase.changeStatus(id, ServiceStatus.COMPLETED, "pro-1", null));
        verify(repository, never()).updateStatusIfMatches(any(), any(), any(Long.class), any());
        verify(publisher, never()).publish(any(), any(), any());
    }

    private void given(ServiceStatus status) {
        Service current = new Service(id, "pro-1", "cli-1", status, 0,
                Instant.parse("2026-10-07T11:00:00Z"), Instant.parse("2026-10-07T11:00:00Z"));
        when(repository.findById(id)).thenReturn(Optional.of(current));
        when(repository.updateStatusIfMatches(eq(id), eq(status), eq(0L), any())).thenAnswer(invocation -> {
            Service updated = invocation.getArgument(3);
            return updated != null;
        });
    }
}
