package com.alamano.core.application.port.in;

import com.alamano.core.domain.professional.NearbyProfessional;
import java.util.List;

public interface FindNearbyProfessionalsUseCase {
    List<NearbyProfessional> findNearby(double latitude, double longitude, Double radiusKm);
}
