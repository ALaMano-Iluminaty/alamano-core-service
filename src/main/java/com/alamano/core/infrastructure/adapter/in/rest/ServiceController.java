package com.alamano.core.infrastructure.adapter.in.rest;

import com.alamano.core.application.port.in.ChangeServiceStatusUseCase;
import com.alamano.core.application.port.in.CreateServiceUseCase;
import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceStateMachine;
import com.alamano.core.domain.service.ServiceStatus;
import com.alamano.core.infrastructure.adapter.in.rest.dto.ChangeServiceStatusRequest;
import com.alamano.core.infrastructure.adapter.in.rest.dto.CreateServiceRequest;
import com.alamano.core.infrastructure.adapter.in.rest.dto.ServiceResponse;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/services")
public class ServiceController {
    private final CreateServiceUseCase createServiceUseCase;
    private final ChangeServiceStatusUseCase changeServiceStatusUseCase;

    public ServiceController(
            CreateServiceUseCase createServiceUseCase, ChangeServiceStatusUseCase changeServiceStatusUseCase) {
        this.createServiceUseCase = createServiceUseCase;
        this.changeServiceStatusUseCase = changeServiceStatusUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse create(@Valid @RequestBody CreateServiceRequest request) {
        Service service = createServiceUseCase.createReserved(request.professionalId(), request.clientId(),
                request.destinationLatitude(), request.destinationLongitude());
        return ServiceResponse.from(service, allowedTransitions(service.status()));
    }

    @PatchMapping("/{serviceId}/status")
    public ServiceResponse changeStatus(
            @PathVariable UUID serviceId,
            @Valid @RequestBody ChangeServiceStatusRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @AuthenticationPrincipal Jwt jwt) {
        Service service = changeServiceStatusUseCase.changeStatus(serviceId, request.status(), jwt.getSubject(), correlationId);
        return ServiceResponse.from(service, allowedTransitions(service.status()));
    }

    private static Set<String> allowedTransitions(ServiceStatus status) {
        return ServiceStateMachine.allowedTargets(status).stream()
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
    }
}
