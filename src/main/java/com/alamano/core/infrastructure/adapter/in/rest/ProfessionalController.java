package com.alamano.core.infrastructure.adapter.in.rest;

import com.alamano.core.application.port.in.ConnectProfessionalUseCase;
import com.alamano.core.application.port.in.FindNearbyProfessionalsUseCase;
import com.alamano.core.domain.professional.NearbyProfessional;
import com.alamano.core.domain.professional.Professional;
import com.alamano.core.infrastructure.adapter.in.rest.dto.ConnectProfessionalRequest;
import com.alamano.core.infrastructure.adapter.in.rest.dto.NearbyProfessionalResponse;
import com.alamano.core.infrastructure.adapter.in.rest.dto.ProfessionalResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/professionals")
public class ProfessionalController {
    private final FindNearbyProfessionalsUseCase findNearbyProfessionalsUseCase;
    private final ConnectProfessionalUseCase connectProfessionalUseCase;

    public ProfessionalController(
            FindNearbyProfessionalsUseCase findNearbyProfessionalsUseCase,
            ConnectProfessionalUseCase connectProfessionalUseCase) {
        this.findNearbyProfessionalsUseCase = findNearbyProfessionalsUseCase;
        this.connectProfessionalUseCase = connectProfessionalUseCase;
    }

    @GetMapping("/nearby")
    public List<NearbyProfessionalResponse> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) Double radiusKm) {
        return findNearbyProfessionalsUseCase.findNearby(lat, lng, radiusKm).stream()
                .map(NearbyProfessionalResponse::from)
                .toList();
    }

    /** El id del vendedor sale del sub del token; nunca se recibe en el cuerpo. */
    @PostMapping("/me/online")
    public ProfessionalResponse goOnline(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConnectProfessionalRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        Professional professional = connectProfessionalUseCase.connect(
                jwt.getSubject(), request.latitude(), request.longitude(), correlationId);
        return ProfessionalResponse.from(professional);
    }
}
