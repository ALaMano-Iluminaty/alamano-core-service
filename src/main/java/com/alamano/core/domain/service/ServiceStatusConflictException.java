package com.alamano.core.domain.service;

import java.util.UUID;

public class ServiceStatusConflictException extends RuntimeException {
    private final UUID serviceId;

    public ServiceStatusConflictException(UUID serviceId) {
        super("El servicio cambió mientras se actualizaba el estado: " + serviceId);
        this.serviceId = serviceId;
    }

    public UUID serviceId() {
        return serviceId;
    }
}
