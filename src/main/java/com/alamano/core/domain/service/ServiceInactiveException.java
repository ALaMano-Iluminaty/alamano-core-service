package com.alamano.core.domain.service;

import java.util.UUID;

public class ServiceInactiveException extends RuntimeException {
    private final UUID serviceId;

    public ServiceInactiveException(UUID serviceId) {
        super("El servicio ya no acepta ubicación: " + serviceId);
        this.serviceId = serviceId;
    }

    public UUID serviceId() {
        return serviceId;
    }
}
