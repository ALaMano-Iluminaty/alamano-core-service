package com.alamano.core.domain.service;

import java.time.Instant;
import java.util.UUID;

public record Service(
        UUID id,
        String professionalId,
        String clientId,
        ServiceStatus status,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    public static Service createReserved(UUID id, String professionalId, String clientId, Instant now) {
        return new Service(id, professionalId, clientId, ServiceStatus.RESERVED, 0, now, now);
    }

    public Service transitionTo(ServiceStatus target) {
        ServiceStatus next = ServiceStateMachine.transition(status, target);
        return new Service(id, professionalId, clientId, next, version + 1, createdAt, updatedAt);
    }

    public Service withUpdatedAt(Instant updatedAt) {
        return new Service(id, professionalId, clientId, status, version, createdAt, updatedAt);
    }
}
