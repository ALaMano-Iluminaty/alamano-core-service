package com.alamano.core.infrastructure.adapter.in.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.alamano.core.application.port.in.UpdateTrackingUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

class LocationUpdatedListenerTest {
    private final UpdateTrackingUseCase useCase = mock(UpdateTrackingUseCase.class);
    private final LocationUpdatedListener listener = new LocationUpdatedListener(useCase);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void validEventCallsTrackingUseCase() throws Exception {
        Instant recordedAt = Instant.parse("2026-01-01T12:00:00Z");
        var payload = mapper.readTree("""
                {"professionalId":"pro-1","serviceId":"00000000-0000-0000-0000-000000000337",
                 "latitude":4.65,"longitude":-74.06,"recordedAt":"2026-01-01T12:00:00Z"}
                """);
        listener.receive(new IncomingEventEnvelope("event-1", "location.updated", 1, recordedAt,
                "corr-1", payload));
        verify(useCase).handleLocation("pro-1", "00000000-0000-0000-0000-000000000337",
                4.65, -74.06, recordedAt, "corr-1");
    }

    @Test
    void rejectsEventMissingServiceId() throws Exception {
        var payload = mapper.readTree("""
                {"professionalId":"pro-1","latitude":4.65,"longitude":-74.06,
                 "recordedAt":"2026-01-01T12:00:00Z"}
                """);
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.receive(
                new IncomingEventEnvelope("event-1", "location.updated", 1, Instant.now(), "corr", payload)));
    }
}
