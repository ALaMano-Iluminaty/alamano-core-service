package com.alamano.core.domain.service;

import java.util.UUID;

public class ServiceNotFoundException extends RuntimeException {
    private final UUID serviceId;

    public ServiceNotFoundException(UUID serviceId) {
        super("Servicio no encontrado: " + serviceId);
        this.serviceId = serviceId;
    }

    public UUID serviceId() {
        return serviceId;
    }
}
