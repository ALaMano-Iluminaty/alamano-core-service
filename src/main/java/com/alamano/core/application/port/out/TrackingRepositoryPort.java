package com.alamano.core.application.port.out;

import com.alamano.core.domain.professional.GeoPoint;
import java.time.Instant;

public interface TrackingRepositoryPort {
    boolean saveLastLocationIfNewer(String serviceId, GeoPoint location, Instant recordedAt);
}
