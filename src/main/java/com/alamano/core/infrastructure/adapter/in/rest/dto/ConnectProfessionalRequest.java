package com.alamano.core.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;

public record ConnectProfessionalRequest(
        @NotNull(message = "La latitud es obligatoria") Double latitude,
        @NotNull(message = "La longitud es obligatoria") Double longitude) {}
