package com.alamano.core.domain.professional;

public class ProfessionalBusyException extends RuntimeException {
    private final String professionalId;

    public ProfessionalBusyException(String professionalId) {
        super("El vendedor tiene un servicio en curso y no puede marcarse disponible");
        this.professionalId = professionalId;
    }

    public String professionalId() {
        return professionalId;
    }
}
