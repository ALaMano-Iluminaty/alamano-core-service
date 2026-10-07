package com.alamano.core.domain.professional;

public class ProfessionalNotFoundException extends RuntimeException {
    public ProfessionalNotFoundException(String professionalId) {
        super("No existe un vendedor con ese id");
    }
}
