package com.alamano.core.infrastructure.adapter.in.rest;

import com.alamano.core.application.port.in.FindNearbyProfessionalsUseCase;
import com.alamano.core.domain.professional.NearbyProfessional;
import com.alamano.core.infrastructure.adapter.in.rest.dto.NearbyProfessionalResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/professionals")
public class ProfessionalController {
    private final FindNearbyProfessionalsUseCase findNearbyProfessionalsUseCase;

    public ProfessionalController(FindNearbyProfessionalsUseCase findNearbyProfessionalsUseCase) {
        this.findNearbyProfessionalsUseCase = findNearbyProfessionalsUseCase;
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
}
