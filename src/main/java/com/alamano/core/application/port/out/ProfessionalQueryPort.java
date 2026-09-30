package com.alamano.core.application.port.out;

import com.alamano.core.domain.professional.NearbyProfessional;
import com.alamano.core.domain.professional.SearchArea;
import java.util.List;

public interface ProfessionalQueryPort {
    List<NearbyProfessional> findAvailableWithin(SearchArea area, int limit);
}
