package com.alamano.core.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public record CreateServiceRequest(
        @NotBlank String professionalId,
        @DecimalMin("-90") @DecimalMax("90") Double destinationLatitude,
        @DecimalMin("-180") @DecimalMax("180") Double destinationLongitude) {
    @AssertTrue(message = "La latitud y longitud de destino deben enviarse juntas.")
    public boolean isDestinationComplete() {
        return (destinationLatitude == null) == (destinationLongitude == null);
    }
}
