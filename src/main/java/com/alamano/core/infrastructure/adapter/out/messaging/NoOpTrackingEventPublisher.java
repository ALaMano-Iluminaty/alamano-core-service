package com.alamano.core.infrastructure.adapter.out.messaging;

import com.alamano.core.application.port.out.TrackingEventPublisherPort;
import com.alamano.core.domain.tracking.TrackingUpdate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class NoOpTrackingEventPublisher implements TrackingEventPublisherPort {
    @Override
    public void publishTrackingUpdated(TrackingUpdate update, String correlationId) {}
}
