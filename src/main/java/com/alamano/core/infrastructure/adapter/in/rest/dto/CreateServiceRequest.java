package com.alamano.core.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateServiceRequest(@NotBlank String professionalId, @NotBlank String clientId) {}
