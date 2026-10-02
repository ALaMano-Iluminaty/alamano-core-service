package com.alamano.core.infrastructure.adapter.out.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.alamano.core.domain.professional.GeoPoint;
import com.alamano.core.domain.professional.DisconnectReason;
import com.alamano.core.domain.professional.Professional;
import com.alamano.core.domain.professional.ProfessionalStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitProfessionalEventPublisherTest {

    @Test
    void publishesOnlineEventToEventsExchange() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitProfessionalEventPublisher publisher =
                new RabbitProfessionalEventPublisher(rabbitTemplate, new ObjectMapper());
        Professional professional = new Professional(
                "pro-1",
                ProfessionalStatus.AVAILABLE,
                new GeoPoint(4.6486, -74.0628),
                Instant.parse("2026-09-30T12:00:00Z"),
                3);

        publisher.publishOnline(professional, "corr-1");

        ArgumentCaptor<DomainEventEnvelope> captor = ArgumentCaptor.forClass(DomainEventEnvelope.class);
        verify(rabbitTemplate).convertAndSend(eq("alamano.events"), eq("professional.online"), captor.capture());
        DomainEventEnvelope envelope = captor.getValue();
        assertNotNull(envelope.eventId());
        assertNotNull(envelope.occurredAt());
        assertEquals("professional.online", envelope.type());
        assertEquals(1, envelope.schemaVersion());
        assertEquals("corr-1", envelope.correlationId());

        JsonNode payload = envelope.payload();
        assertEquals("pro-1", payload.get("professionalId").asText());
        assertEquals(4.6486, payload.get("latitude").asDouble());
        assertEquals(-74.0628, payload.get("longitude").asDouble());
        assertEquals("AVAILABLE", payload.get("status").asText());
        assertEquals(3, payload.get("version").asLong());
    }

    @Test
    void publishesDisconnectedEventWithReasonAndOfflineStatus() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitProfessionalEventPublisher publisher = new RabbitProfessionalEventPublisher(rabbitTemplate, new ObjectMapper());
        Professional professional = new Professional(
                "pro-1", ProfessionalStatus.OFFLINE, new GeoPoint(4.6486, -74.0628),
                Instant.parse("2026-09-30T12:00:00Z"), 4);

        publisher.publishDisconnected(professional, DisconnectReason.CONNECTION_LOST, "corr-lost");

        ArgumentCaptor<DomainEventEnvelope> captor = ArgumentCaptor.forClass(DomainEventEnvelope.class);
        verify(rabbitTemplate).convertAndSend(eq("alamano.events"), eq("professional.disconnected"), captor.capture());
        DomainEventEnvelope envelope = captor.getValue();
        assertNotNull(envelope.eventId());
        assertEquals("professional.disconnected", envelope.type());
        assertEquals("corr-lost", envelope.correlationId());
        assertEquals("pro-1", envelope.payload().get("professionalId").asText());
        assertEquals("OFFLINE", envelope.payload().get("status").asText());
        assertEquals(4, envelope.payload().get("version").asLong());
        assertEquals("CONNECTION_LOST", envelope.payload().get("reason").asText());
    }
}
