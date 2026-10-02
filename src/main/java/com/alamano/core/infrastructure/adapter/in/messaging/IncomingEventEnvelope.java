package com.alamano.core.infrastructure.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record IncomingEventEnvelope(
        String eventId, String type, int schemaVersion, Instant occurredAt, String correlationId, JsonNode payload) {}
