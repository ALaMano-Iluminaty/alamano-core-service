package com.alamano.core.domain.service;

public class InvalidServiceStatusTransitionException extends RuntimeException {
    private final ServiceStatus current;
    private final ServiceStatus target;

    public InvalidServiceStatusTransitionException(ServiceStatus current, ServiceStatus target) {
        super("Transición inválida de " + current + " a " + target);
        this.current = current;
        this.target = target;
    }

    public ServiceStatus current() {
        return current;
    }

    public ServiceStatus target() {
        return target;
    }
}
