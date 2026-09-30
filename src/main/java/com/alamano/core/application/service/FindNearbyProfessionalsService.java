package com.alamano.core.application.service;

import com.alamano.core.application.port.in.FindNearbyProfessionalsUseCase;
import com.alamano.core.application.port.out.ProfessionalQueryPort;
import com.alamano.core.domain.professional.GeoPoint;
import com.alamano.core.domain.professional.NearbyProfessional;
import com.alamano.core.domain.professional.SearchArea;
import java.util.List;

public class FindNearbyProfessionalsService implements FindNearbyProfessionalsUseCase {
    public static final int MAX_RESULTS = 50;

    private final ProfessionalQueryPort professionalQuery;

    public FindNearbyProfessionalsService(ProfessionalQueryPort professionalQuery) {
        this.professionalQuery = professionalQuery;
    }

    @Override
    public List<NearbyProfessional> findNearby(double latitude, double longitude, Double radiusKm) {
        GeoPoint center = new GeoPoint(latitude, longitude);
        SearchArea area = new SearchArea(
                center,
                radiusKm == null ? SearchArea.DEFAULT_RADIUS_KM : radiusKm);
        return professionalQuery.findAvailableWithin(area, MAX_RESULTS);
    }
}
