package com.alamano.core.domain.professional;

public class ProfessionalConcurrentUpdateException extends RuntimeException {
    private final String professionalId;

    public ProfessionalConcurrentUpdateException(String professionalId) {
        super("El vendedor fue modificado al mismo tiempo; intenta de nuevo");
        this.professionalId = professionalId;
    }

    public String professionalId() {
        return professionalId;
    }
}
