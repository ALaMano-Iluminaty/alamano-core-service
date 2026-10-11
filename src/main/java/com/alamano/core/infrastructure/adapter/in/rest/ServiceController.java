package com.alamano.core.infrastructure.adapter.in.rest;

import com.alamano.core.application.port.in.ChangeServiceStatusUseCase;
import com.alamano.core.application.port.in.CreateServiceUseCase;
import com.alamano.core.application.port.in.GetServiceUseCase;
import com.alamano.core.application.port.in.ReportServiceLocationUseCase;
import com.alamano.core.domain.service.Service;
import com.alamano.core.infrastructure.adapter.in.rest.dto.ChangeServiceStatusRequest;
import com.alamano.core.infrastructure.adapter.in.rest.dto.CreateServiceRequest;
import com.alamano.core.infrastructure.adapter.in.rest.dto.ServiceLocationRequest;
import com.alamano.core.infrastructure.adapter.in.rest.dto.ServiceResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/services")
public class ServiceController {
    private final CreateServiceUseCase createServiceUseCase;
    private final ChangeServiceStatusUseCase changeServiceStatusUseCase;
    private final GetServiceUseCase getServiceUseCase;
    private final ReportServiceLocationUseCase reportServiceLocationUseCase;
    private final ServiceResponseAssembler responses;
    private final Clock clock;

    public ServiceController(
            CreateServiceUseCase createServiceUseCase,
            ChangeServiceStatusUseCase changeServiceStatusUseCase,
            GetServiceUseCase getServiceUseCase,
            ReportServiceLocationUseCase reportServiceLocationUseCase,
            ServiceResponseAssembler responses,
            Clock clock) {
        this.createServiceUseCase = createServiceUseCase;
        this.changeServiceStatusUseCase = changeServiceStatusUseCase;
        this.getServiceUseCase = getServiceUseCase;
        this.reportServiceLocationUseCase = reportServiceLocationUseCase;
        this.responses = responses;
        this.clock = clock;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse create(
            @Valid @RequestBody CreateServiceRequest request, @AuthenticationPrincipal Jwt jwt) {
        Service service = createServiceUseCase.createReserved(
                request.professionalId(),
                jwt.getSubject(),
                request.destinationLatitude(),
                request.destinationLongitude());
        return responses.from(service);
    }

    @GetMapping("/{serviceId}")
    public ServiceResponse get(@PathVariable UUID serviceId, @AuthenticationPrincipal Jwt jwt) {
        return responses.from(getServiceUseCase.get(serviceId, jwt.getSubject()));
    }

    @PatchMapping("/{serviceId}/status")
    public ServiceResponse changeStatus(
            @PathVariable UUID serviceId,
            @Valid @RequestBody ChangeServiceStatusRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @AuthenticationPrincipal Jwt jwt) {
        Service service = changeServiceStatusUseCase.changeStatus(
                serviceId, request.status(), jwt.getSubject(), correlationId);
        return responses.from(service);
    }

    @PostMapping("/{serviceId}/location")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reportLocation(
            @PathVariable UUID serviceId,
            @Valid @RequestBody ServiceLocationRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @AuthenticationPrincipal Jwt jwt) {
        Instant recordedAt = request.at() == null ? clock.instant() : request.at();
        reportServiceLocationUseCase.report(
                serviceId, jwt.getSubject(), request.lat(), request.lng(), recordedAt, correlationId);
    }
}
