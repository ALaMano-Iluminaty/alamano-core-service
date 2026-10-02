package com.alamano.core.infrastructure.adapter.in.messaging;

import com.alamano.core.application.port.in.DisconnectProfessionalUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ProfessionalConnectionListener {
    private static final Logger log = LoggerFactory.getLogger(ProfessionalConnectionListener.class);
    private final DisconnectProfessionalUseCase disconnectProfessionalUseCase;

    public ProfessionalConnectionListener(DisconnectProfessionalUseCase disconnectProfessionalUseCase) {
        this.disconnectProfessionalUseCase = disconnectProfessionalUseCase;
    }

    @RabbitListener(queues = "#{professionalConnectionQueue.name}")
    public void receive(IncomingEventEnvelope event) {
        String professionalId = event == null || event.payload() == null
                ? null
                : event.payload().path("professionalId").asText(null);
        if (event == null || event.eventId() == null || event.occurredAt() == null
                || professionalId == null || professionalId.isBlank()) {
            throw new AmqpRejectAndDontRequeueException("El evento de conexión perdida está incompleto.");
        }
        try {
            if (event.correlationId() != null) MDC.put("correlationId", event.correlationId());
            MDC.put("eventId", event.eventId());
            log.info("Evento recibido: {}", event.type());
            disconnectProfessionalUseCase.handleConnectionLost(professionalId, event.occurredAt(), event.correlationId());
        } finally {
            MDC.remove("correlationId");
            MDC.remove("eventId");
        }
    }
}
