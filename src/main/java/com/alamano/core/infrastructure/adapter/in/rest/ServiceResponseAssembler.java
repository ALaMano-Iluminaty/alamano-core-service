package com.alamano.core.infrastructure.adapter.in.rest;

import com.alamano.core.application.port.out.ProfessionalRepositoryPort;
import com.alamano.core.domain.professional.GeoPoint;
import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.service.ServiceStateMachine;
import com.alamano.core.domain.tracking.EtaCalculator;
import com.alamano.core.infrastructure.adapter.in.rest.dto.ProfessionalSummaryResponse;
import com.alamano.core.infrastructure.adapter.in.rest.dto.ServiceResponse;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ServiceResponseAssembler {
    private final EtaCalculator etaCalculator;
    private final ProfessionalRepositoryPort professionals;

    public ServiceResponseAssembler(EtaCalculator etaCalculator, ProfessionalRepositoryPort professionals) {
        this.etaCalculator = etaCalculator;
        this.professionals = professionals;
    }

    public ServiceResponse from(Service service) {
        GeoPoint last = service.lastLatitude() == null || service.lastLongitude() == null
                ? null
                : new GeoPoint(service.lastLatitude(), service.lastLongitude());
        GeoPoint destination = service.destinationLatitude() == null || service.destinationLongitude() == null
                ? null
                : new GeoPoint(service.destinationLatitude(), service.destinationLongitude());
        Integer etaSeconds = last == null
                ? null
                : etaCalculator.etaSeconds(last, destination, service.status().name());
        ProfessionalSummaryResponse professional = professionals
                .findById(service.professionalId())
                .map(pro -> new ProfessionalSummaryResponse(pro.id(), pro.status().name()))
                .orElse(new ProfessionalSummaryResponse(service.professionalId(), null));
        return ServiceResponse.from(service, allowedTransitions(service), etaSeconds, professional);
    }

    private static Set<String> allowedTransitions(Service service) {
        return ServiceStateMachine.allowedTargets(service.status()).stream()
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
    }
}
