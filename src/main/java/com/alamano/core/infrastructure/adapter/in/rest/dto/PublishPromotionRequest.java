package com.alamano.core.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PublishPromotionRequest(
        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String description,
        @NotNull(message = "El número de cupos es obligatorio")
        @Min(value = 1, message = "Los cupos deben ser al menos 1")
        @Max(value = 10000, message = "Los cupos no pueden superar 10000")
        Integer totalSlots) {}
