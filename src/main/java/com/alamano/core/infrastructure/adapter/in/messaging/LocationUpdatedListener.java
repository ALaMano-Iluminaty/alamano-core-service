package com.alamano.core.infrastructure.adapter.in.messaging;

import com.alamano.core.application.port.in.UpdateTrackingUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class LocationUpdatedListener {
    private static final Logger log = LoggerFactory.getLogger(LocationUpdatedListener.class);
    private final UpdateTrackingUseCase updateTrackingUseCase;

    public LocationUpdatedListener(UpdateTrackingUseCase updateTrackingUseCase) {
        this.updateTrackingUseCase = updateTrackingUseCase;
    }

    @RabbitListener(queues = "#{locationUpdatesQueue.name}")
    public void receive(IncomingEventEnvelope event) {
        JsonNode payload = event == null ? null : event.payload();
        if (event == null || payload == null || !text(payload, "professionalId")
                || !text(payload, "serviceId") || !number(payload, "latitude")
                || !number(payload, "longitude") || !text(payload, "recordedAt")) {
            throw new AmqpRejectAndDontRequeueException("El evento location.updated está incompleto.");
        }
        Instant recordedAt;
        try {
            recordedAt = Instant.parse(payload.get("recordedAt").asText());
        } catch (DateTimeParseException exception) {
            throw new AmqpRejectAndDontRequeueException("La fecha recordedAt no es válida.", exception);
        }
        try {
            if (event.correlationId() != null) MDC.put("correlationId", event.correlationId());
            if (event.eventId() != null) MDC.put("eventId", event.eventId());
            log.debug("Evento recibido: {}", event.type());
            updateTrackingUseCase.handleLocation(payload.get("professionalId").asText(),
                    payload.get("serviceId").asText(), payload.get("latitude").asDouble(),
                    payload.get("longitude").asDouble(), recordedAt, event.correlationId());
        } finally {
            MDC.remove("correlationId");
            MDC.remove("eventId");
        }
    }

    private static boolean text(JsonNode payload, String field) {
        return payload.hasNonNull(field) && payload.get(field).isTextual() && !payload.get(field).asText().isBlank();
    }

    private static boolean number(JsonNode payload, String field) {
        return payload.hasNonNull(field) && payload.get(field).isNumber()
                && Double.isFinite(payload.get(field).asDouble());
    }
}
