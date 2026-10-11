package com.alamano.core.infrastructure.adapter.in.rest;

import com.alamano.core.application.port.in.ConnectProfessionalUseCase;
import com.alamano.core.application.port.in.DisconnectProfessionalUseCase;
import com.alamano.core.application.port.in.FindNearbyProfessionalsUseCase;
import com.alamano.core.application.port.in.ListPromotionsUseCase;
import com.alamano.core.domain.professional.NearbyProfessional;
import com.alamano.core.domain.professional.Professional;
import com.alamano.core.domain.promotion.PromotionAvailability;
import com.alamano.core.infrastructure.adapter.in.rest.dto.ConnectProfessionalRequest;
import com.alamano.core.infrastructure.adapter.in.rest.dto.NearbyProfessionalResponse;
import com.alamano.core.infrastructure.adapter.in.rest.dto.ProfessionalResponse;
import com.alamano.core.infrastructure.adapter.in.rest.dto.PromotionResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final DisconnectProfessionalUseCase disconnectProfessionalUseCase;
    private final ListPromotionsUseCase listPromotionsUseCase;

    public ProfessionalController(
            FindNearbyProfessionalsUseCase findNearbyProfessionalsUseCase,
            ConnectProfessionalUseCase connectProfessionalUseCase,
            DisconnectProfessionalUseCase disconnectProfessionalUseCase,
            ListPromotionsUseCase listPromotionsUseCase) {
        this.findNearbyProfessionalsUseCase = findNearbyProfessionalsUseCase;
        this.connectProfessionalUseCase = connectProfessionalUseCase;
        this.disconnectProfessionalUseCase = disconnectProfessionalUseCase;
        this.listPromotionsUseCase = listPromotionsUseCase;
    }

    @GetMapping("/nearby")
    public List<NearbyProfessionalResponse> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) Double radiusKm) {
        List<NearbyProfessional> nearby = findNearbyProfessionalsUseCase.findNearby(lat, lng, radiusKm);
        Map<String, List<PromotionAvailability>> promotions = listPromotionsUseCase.byProfessionals(
                nearby.stream().map(NearbyProfessional::professionalId).toList());
        return nearby.stream()
                .map(professional -> NearbyProfessionalResponse.from(
                        professional, promotions.getOrDefault(professional.professionalId(), List.of())))
                .toList();
    }

    @GetMapping("/{professionalId}/promotions")
    public List<PromotionResponse> promotions(@PathVariable String professionalId) {
        return listPromotionsUseCase.byProfessional(professionalId).stream()
                .map(PromotionResponse::from)
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

    /** El id sale del sub del token y el request no necesita cuerpo. */
    @PostMapping("/me/offline")
    public ProfessionalResponse goOffline(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return ProfessionalResponse.from(disconnectProfessionalUseCase.goOffline(jwt.getSubject(), correlationId));
    }
}
