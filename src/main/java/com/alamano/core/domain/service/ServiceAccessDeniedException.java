package com.alamano.core.domain.service;

public class ServiceAccessDeniedException extends RuntimeException {
    public ServiceAccessDeniedException() {
        super("No puedes cambiar el estado de este servicio");
    }
}
