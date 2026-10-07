package com.alamano.core.domain.tracking;

import com.alamano.core.domain.professional.GeoPoint;
import java.time.Instant;

public record TrackingUpdate(String serviceId, String professionalId, GeoPoint location,
        Integer etaSeconds, Instant recordedAt) {}
