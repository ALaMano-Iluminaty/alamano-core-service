package com.alamano.core.application.port.out;

import com.alamano.core.domain.tracking.TrackingUpdate;

public interface TrackingEventPublisherPort {
    void publishTrackingUpdated(TrackingUpdate update, String correlationId);
}
