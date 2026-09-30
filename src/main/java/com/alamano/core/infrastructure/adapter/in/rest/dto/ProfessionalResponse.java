package com.alamano.core.infrastructure.adapter.in.rest.dto;

import com.alamano.core.domain.professional.Professional;
import com.alamano.core.domain.professional.ProfessionalStatus;

public record ProfessionalResponse(
        String professionalId, ProfessionalStatus status, Double latitude, Double longitude, long version) {

    public static ProfessionalResponse from(Professional professional) {
        return new ProfessionalResponse(
                professional.id(),
                professional.status(),
                professional.location() == null ? null : professional.location().latitude(),
                professional.location() == null ? null : professional.location().longitude(),
                professional.version());
    }
}
