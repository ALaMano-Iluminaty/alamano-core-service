package com.alamano.core.infrastructure.adapter.out.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.alamano.core.domain.professional.GeoPoint;
import com.alamano.core.domain.tracking.TrackingUpdate;
import com.alamano.core.infrastructure.config.RabbitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitTrackingEventPublisherTest {
    @Test
    void publishesTrackingUpdateEnvelope() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        RabbitTrackingEventPublisher publisher = new RabbitTrackingEventPublisher(rabbit, new ObjectMapper());
        Instant recordedAt = Instant.parse("2026-01-01T12:00:00Z");
        TrackingUpdate update = new TrackingUpdate("service-1", "pro-1", new GeoPoint(4.65, -74.06),
                720, recordedAt);

        publisher.publishTrackingUpdated(update, "corr-1");

        ArgumentCaptor<DomainEventEnvelope> captor = ArgumentCaptor.forClass(DomainEventEnvelope.class);
        verify(rabbit).convertAndSend(eq(RabbitConfig.EVENTS_EXCHANGE),
                eq(RabbitTrackingEventPublisher.EVENT_TYPE), captor.capture());
        DomainEventEnvelope envelope = captor.getValue();
        assertEquals("tracking.updated", envelope.type());
        assertEquals(1, envelope.schemaVersion());
        assertNotNull(envelope.eventId());
        assertEquals("corr-1", envelope.correlationId());
        assertEquals("service-1", envelope.payload().path("serviceId").asText());
        assertEquals("pro-1", envelope.payload().path("professionalId").asText());
        assertEquals(4.65, envelope.payload().path("latitude").asDouble());
        assertEquals(-74.06, envelope.payload().path("longitude").asDouble());
        assertEquals(720, envelope.payload().path("etaSeconds").asInt());
        assertEquals(recordedAt.toString(), envelope.payload().path("recordedAt").asText());
    }
}
