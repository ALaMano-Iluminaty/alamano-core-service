package com.alamano.core.infrastructure.adapter.out.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record DomainEventEnvelope(
        String eventId, String type, int schemaVersion, Instant occurredAt, String correlationId, JsonNode payload) {}
