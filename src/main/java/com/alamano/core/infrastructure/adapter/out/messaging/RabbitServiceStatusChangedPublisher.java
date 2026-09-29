package com.alamano.core.infrastructure.adapter.out.messaging;

import com.alamano.core.application.port.out.ServiceStatusChangedPublisherPort;
import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceStatus;
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
public class RabbitServiceStatusChangedPublisher implements ServiceStatusChangedPublisherPort {
    public static final String EVENT_TYPE = "service.status.changed";
    public static final int SCHEMA_VERSION = 1;

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public RabbitServiceStatusChangedPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(Service service, ServiceStatus previousStatus, String correlationId) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("serviceId", service.id().toString());
        payload.put("professionalId", service.professionalId());
        payload.put("clientId", service.clientId());
        payload.put("previousStatus", previousStatus.name());
        payload.put("status", service.status().name());
        payload.put("version", service.version());

        DomainEventEnvelope envelope = new DomainEventEnvelope(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                SCHEMA_VERSION,
                Instant.now(),
                correlationId,
                payload);

        rabbitTemplate.convertAndSend(RabbitConfig.EVENTS_EXCHANGE, EVENT_TYPE, envelope);
    }
}
