package com.alamano.core.infrastructure.adapter.out.messaging;

import com.alamano.core.application.port.out.ProfessionalEventPublisherPort;
import com.alamano.core.domain.professional.Professional;
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
public class RabbitProfessionalEventPublisher implements ProfessionalEventPublisherPort {
    public static final String ONLINE_EVENT_TYPE = "professional.online";
    public static final int SCHEMA_VERSION = 1;

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public RabbitProfessionalEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishOnline(Professional professional, String correlationId) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("professionalId", professional.id());
        payload.put("latitude", professional.location().latitude());
        payload.put("longitude", professional.location().longitude());
        payload.put("status", professional.status().name());
        payload.put("version", professional.version());

        DomainEventEnvelope envelope = new DomainEventEnvelope(
                UUID.randomUUID().toString(),
                ONLINE_EVENT_TYPE,
                SCHEMA_VERSION,
                Instant.now(),
                correlationId,
                payload);

        rabbitTemplate.convertAndSend(RabbitConfig.EVENTS_EXCHANGE, ONLINE_EVENT_TYPE, envelope);
    }
}
