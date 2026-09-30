package com.alamano.core.infrastructure.adapter.out.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

class DomainEventEnvelopeSerializationTest {

    @Test
    void serializesEnvelopeAsJsonWithIso8601Timestamp() throws Exception {
        ObjectMapper objectMapper = JsonMapper.builder()
                .findAndAddModules()
                // Jackson serializa Instant como número si WRITE_DATES_AS_TIMESTAMPS sigue activo.
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        Instant occurredAt = Instant.parse("2026-09-29T12:34:56Z");
        DomainEventEnvelope envelope = new DomainEventEnvelope(
                "event-1",
                "service.status.changed",
                1,
                occurredAt,
                "correlation-1",
                objectMapper.createObjectNode().put("serviceId", "service-1"));

        Message message = converter.toMessage(envelope, new MessageProperties());
        var json = objectMapper.readTree(message.getBody());

        assertEquals(MessageProperties.CONTENT_TYPE_JSON, message.getMessageProperties().getContentType());
        assertTrue(json.has("eventId"));
        assertTrue(json.has("type"));
        assertTrue(json.has("schemaVersion"));
        assertTrue(json.has("occurredAt"));
        assertTrue(json.has("correlationId"));
        assertTrue(json.has("payload"));
        assertTrue(json.get("occurredAt").isTextual());
        assertEquals("2026-09-29T12:34:56Z", json.get("occurredAt").asText());
    }
}
