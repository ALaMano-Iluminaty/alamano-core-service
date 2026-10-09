package com.alamano.core.infrastructure.config;

import com.alamano.core.application.port.in.ChangeServiceStatusUseCase;
import com.alamano.core.application.port.in.ConnectProfessionalUseCase;
import com.alamano.core.application.port.in.DisconnectProfessionalUseCase;
import com.alamano.core.application.port.in.CreateServiceUseCase;
import com.alamano.core.application.port.in.FindNearbyProfessionalsUseCase;
import com.alamano.core.application.port.in.UpdateTrackingUseCase;
import com.alamano.core.application.port.out.ProfessionalEventPublisherPort;
import com.alamano.core.application.port.out.ProfessionalQueryPort;
import com.alamano.core.application.port.out.ProfessionalRepositoryPort;
import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.application.port.out.ServiceStatusChangedPublisherPort;
import com.alamano.core.application.port.out.TrackingEventPublisherPort;
import com.alamano.core.application.port.out.TrackingRepositoryPort;
import com.alamano.core.application.service.ChangeServiceStatusService;
import com.alamano.core.application.service.ConnectProfessionalService;
import com.alamano.core.application.service.DisconnectProfessionalService;
import com.alamano.core.application.service.CreateServiceService;
import com.alamano.core.application.service.FindNearbyProfessionalsService;
import com.alamano.core.application.service.UpdateTrackingService;
import com.alamano.core.domain.tracking.EtaCalculator;
import com.alamano.core.application.port.in.GetPromotionUseCase;
import com.alamano.core.application.port.in.PublishPromotionUseCase;
import com.alamano.core.application.port.out.PromotionCounterPort;
import com.alamano.core.application.port.out.PromotionRepositoryPort;
import com.alamano.core.application.service.GetPromotionService;
import com.alamano.core.application.service.PublishPromotionService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class ApplicationConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ChangeServiceStatusUseCase changeServiceStatusUseCase(
            ServiceRepositoryPort repository,
            ServiceStatusChangedPublisherPort publisher,
            Clock clock) {
        return new ChangeServiceStatusService(repository, publisher, clock);
    }

    @Bean
    CreateServiceUseCase createServiceUseCase(
            ServiceRepositoryPort repository, ServiceStatusChangedPublisherPort publisher, Clock clock) {
        return new CreateServiceService(repository, publisher, clock);
    }

    @Bean
    EtaCalculator etaCalculator(@Value("${alamano.tracking.average-speed-kmh:25}") double averageSpeedKmh) {
        return new EtaCalculator(averageSpeedKmh);
    }

    @Bean
    UpdateTrackingUseCase updateTrackingUseCase(ServiceRepositoryPort serviceRepository,
            TrackingRepositoryPort trackingRepository, TrackingEventPublisherPort publisher,
            EtaCalculator etaCalculator) {
        return new UpdateTrackingService(serviceRepository, trackingRepository, publisher, etaCalculator);
    }

    @Bean
    FindNearbyProfessionalsUseCase findNearbyProfessionalsUseCase(ProfessionalQueryPort professionalQuery) {
        return new FindNearbyProfessionalsService(professionalQuery);
    }

    @Bean
    ConnectProfessionalUseCase connectProfessionalUseCase(
            ProfessionalRepositoryPort repository,
            ProfessionalEventPublisherPort publisher,
            Clock clock) {
        return new ConnectProfessionalService(repository, publisher, clock);
    }

    @Bean
    DisconnectProfessionalUseCase disconnectProfessionalUseCase(
            ProfessionalRepositoryPort repository,
            ProfessionalEventPublisherPort publisher,
            Clock clock) {
        return new DisconnectProfessionalService(repository, publisher, clock);
    }

    @Bean
    PublishPromotionUseCase publishPromotionUseCase(
            PromotionRepositoryPort repository, PromotionCounterPort counter, Clock clock) {
        return new PublishPromotionService(repository, counter, clock);
    }

    @Bean
    GetPromotionUseCase getPromotionUseCase(
            PromotionRepositoryPort repository, PromotionCounterPort counter) {
        return new GetPromotionService(repository, counter);
    }
}
