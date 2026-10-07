package com.alamano.core.infrastructure.adapter.in.messaging;

import com.alamano.core.infrastructure.config.RabbitConfig;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class RabbitListenerConfig {
    public static final String PROFESSIONAL_CONNECTION_QUEUE = "core.professional-connection";
    public static final String DEAD_LETTER_EXCHANGE = "alamano.events.dlx";

    @Bean
    Queue professionalConnectionQueue() {
        return QueueBuilder.durable(PROFESSIONAL_CONNECTION_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey("core.dlq")
                .build();
    }

    @Bean
    Binding professionalConnectionLostBinding(Queue professionalConnectionQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(professionalConnectionQueue)
                .to(eventsExchange)
                .with("professional.connection.lost");
    }
}
