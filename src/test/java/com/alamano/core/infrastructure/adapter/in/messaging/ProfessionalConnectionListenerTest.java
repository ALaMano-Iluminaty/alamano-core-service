package com.alamano.core.infrastructure.adapter.in.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.alamano.core.application.port.in.DisconnectProfessionalUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

class ProfessionalConnectionListenerTest {
    @Test
    void validEventCallsConnectionLostUseCase() {
        DisconnectProfessionalUseCase useCase = mock(DisconnectProfessionalUseCase.class);
        ProfessionalConnectionListener listener = new ProfessionalConnectionListener(useCase);
        Instant occurredAt = Instant.parse("2026-09-30T12:00:00Z");
        var payload = new ObjectMapper().createObjectNode().put("professionalId", "pro-1");
        var event = new IncomingEventEnvelope("event-1", "professional.connection.lost", 1,
                occurredAt, "corr-1", payload);
        listener.receive(event);
        verify(useCase).handleConnectionLost("pro-1", occurredAt, "corr-1");
    }

    @Test
    void missingProfessionalIdIsRejectedWithoutCallingUseCase() {
        DisconnectProfessionalUseCase useCase = mock(DisconnectProfessionalUseCase.class);
        ProfessionalConnectionListener listener = new ProfessionalConnectionListener(useCase);
        var event = new IncomingEventEnvelope("event-1", "professional.connection.lost", 1,
                Instant.parse("2026-09-30T12:00:00Z"), "corr-1", new ObjectMapper().createObjectNode());
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.receive(event));
        verifyNoInteractions(useCase);
    }
}
