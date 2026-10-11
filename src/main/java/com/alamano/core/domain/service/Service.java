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
        Instant updatedAt,
        Double destinationLatitude,
        Double destinationLongitude,
        Double lastLatitude,
        Double lastLongitude,
        Instant lastTrackedAt) {

    public Service(UUID id, String professionalId, String clientId, ServiceStatus status,
            long version, Instant createdAt, Instant updatedAt) {
        this(id, professionalId, clientId, status, version, createdAt, updatedAt, null, null, null, null, null);
    }

    public Service(UUID id, String professionalId, String clientId, ServiceStatus status,
            long version, Instant createdAt, Instant updatedAt,
            Double destinationLatitude, Double destinationLongitude) {
        this(id, professionalId, clientId, status, version, createdAt, updatedAt,
                destinationLatitude, destinationLongitude, null, null, null);
    }

    public static Service createReserved(UUID id, String professionalId, String clientId, Instant now) {
        return createReserved(id, professionalId, clientId, now, null, null);
    }

    public static Service createReserved(UUID id, String professionalId, String clientId, Instant now,
            Double destinationLatitude, Double destinationLongitude) {
        return new Service(id, professionalId, clientId, ServiceStatus.RESERVED, 0, now, now,
                destinationLatitude, destinationLongitude, null, null, null);
    }

    public Service transitionTo(ServiceStatus target) {
        ServiceStatus next = ServiceStateMachine.transition(status, target);
        return new Service(id, professionalId, clientId, next, version + 1, createdAt, updatedAt,
                destinationLatitude, destinationLongitude, lastLatitude, lastLongitude, lastTrackedAt);
    }

    public Service withUpdatedAt(Instant updatedAt) {
        return new Service(id, professionalId, clientId, status, version, createdAt, updatedAt,
                destinationLatitude, destinationLongitude, lastLatitude, lastLongitude, lastTrackedAt);
    }

    public boolean isTerminal() {
        return status == ServiceStatus.COMPLETED || status == ServiceStatus.CANCELLED;
    }

    public boolean isParticipant(String userId) {
        return professionalId.equals(userId) || clientId.equals(userId);
    }
}
