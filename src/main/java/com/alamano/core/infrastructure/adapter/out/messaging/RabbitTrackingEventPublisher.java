package com.alamano.core.infrastructure.adapter.out.messaging;

import com.alamano.core.application.port.out.TrackingEventPublisherPort;
import com.alamano.core.domain.tracking.TrackingUpdate;
import com.alamano.core.infrastructure.config.RabbitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class RabbitTrackingEventPublisher implements TrackingEventPublisherPort {
    public static final String EVENT_TYPE = "tracking.updated";
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public RabbitTrackingEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishTrackingUpdated(TrackingUpdate update, String correlationId) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("serviceId", update.serviceId());
        payload.put("professionalId", update.professionalId());
        payload.put("latitude", update.location().latitude());
        payload.put("longitude", update.location().longitude());
        if (update.etaSeconds() == null) payload.putNull("etaSeconds");
        else payload.put("etaSeconds", update.etaSeconds());
        payload.put("recordedAt", update.recordedAt().toString());
        var envelope = new DomainEventEnvelope(UUID.randomUUID().toString(), EVENT_TYPE, 1,
                Instant.now(), correlationId, payload);
        rabbitTemplate.convertAndSend(RabbitConfig.EVENTS_EXCHANGE, EVENT_TYPE, envelope);
    }
}
