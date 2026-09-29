package com.alamano.core.infrastructure.adapter.in.rest.dto;

import com.alamano.core.domain.service.ServiceStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeServiceStatusRequest(@NotNull ServiceStatus status) {}
