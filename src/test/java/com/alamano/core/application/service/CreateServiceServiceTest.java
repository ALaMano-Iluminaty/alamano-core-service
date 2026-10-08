package com.alamano.core.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.application.port.out.ServiceStatusChangedPublisherPort;
import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class CreateServiceServiceTest {
    @Test
    void savesBeforePublishingInitialReservedEvent() {
        ServiceRepositoryPort repository = mock(ServiceRepositoryPort.class);
        ServiceStatusChangedPublisherPort publisher = mock(ServiceStatusChangedPublisherPort.class);
        Clock clock = Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC);
        when(repository.save(any(Service.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateServiceService service = new CreateServiceService(repository, publisher, clock);

        Service created = service.createReserved("pro-1", "cli-1");

        InOrder order = inOrder(repository, publisher);
        order.verify(repository).save(created);
        order.verify(publisher).publish(created, null, null);
        assertEquals(ServiceStatus.RESERVED, created.status());
        assertEquals(0, created.version());
        assertNull(created.destinationLatitude());
    }
}
