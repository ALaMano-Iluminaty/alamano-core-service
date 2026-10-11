package com.alamano.core.infrastructure.config;

import com.alamano.core.application.port.in.ChangeServiceStatusUseCase;
import com.alamano.core.application.port.in.ClaimPromotionUseCase;
import com.alamano.core.application.port.in.ConnectProfessionalUseCase;
import com.alamano.core.application.port.in.CreateServiceUseCase;
import com.alamano.core.application.port.in.DisconnectProfessionalUseCase;
import com.alamano.core.application.port.in.FindNearbyProfessionalsUseCase;
import com.alamano.core.application.port.in.GetPromotionUseCase;
import com.alamano.core.application.port.in.GetServiceUseCase;
import com.alamano.core.application.port.in.ListPromotionsUseCase;
import com.alamano.core.application.port.in.PublishPromotionUseCase;
import com.alamano.core.application.port.in.ReportServiceLocationUseCase;
import com.alamano.core.application.port.in.UpdateTrackingUseCase;
import com.alamano.core.application.port.out.ProfessionalEventPublisherPort;
import com.alamano.core.application.port.out.ProfessionalQueryPort;
import com.alamano.core.application.port.out.ProfessionalRepositoryPort;
import com.alamano.core.application.port.out.PromotionClaimPort;
import com.alamano.core.application.port.out.PromotionCounterPort;
import com.alamano.core.application.port.out.PromotionRepositoryPort;
import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.application.port.out.ServiceStatusChangedPublisherPort;
import com.alamano.core.application.port.out.TrackingEventPublisherPort;
import com.alamano.core.application.port.out.TrackingRepositoryPort;
import com.alamano.core.application.service.ChangeServiceStatusService;
import com.alamano.core.application.service.ClaimPromotionService;
import com.alamano.core.application.service.ConnectProfessionalService;
import com.alamano.core.application.service.CreateServiceService;
import com.alamano.core.application.service.DisconnectProfessionalService;
import com.alamano.core.application.service.FindNearbyProfessionalsService;
import com.alamano.core.application.service.GetPromotionService;
import com.alamano.core.application.service.GetServiceService;
import com.alamano.core.application.service.ListPromotionsService;
import com.alamano.core.application.service.PublishPromotionService;
import com.alamano.core.application.service.ReportServiceLocationService;
import com.alamano.core.application.service.UpdateTrackingService;
import com.alamano.core.domain.service.Service;
import com.alamano.core.domain.tracking.EtaCalculator;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class ApplicationConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    ChangeServiceStatusUseCase changeServiceStatusUseCase(
            ServiceRepositoryPort repository,
            ProfessionalRepositoryPort professionals,
            ServiceStatusChangedPublisherPort publisher,
            Clock clock,
            TransactionTemplate transactionTemplate) {
        ChangeServiceStatusService inner = new ChangeServiceStatusService(repository, professionals, publisher, clock);
        return (serviceId, targetStatus, userId, correlationId) ->
                transactionTemplate.execute(status -> inner.changeStatus(serviceId, targetStatus, userId, correlationId));
    }

    @Bean
    CreateServiceUseCase createServiceUseCase(
            ServiceRepositoryPort repository,
            ProfessionalRepositoryPort professionals,
            ServiceStatusChangedPublisherPort publisher,
            Clock clock,
            TransactionTemplate transactionTemplate) {
        CreateServiceService inner = new CreateServiceService(repository, professionals, publisher, clock);
        return new CreateServiceUseCase() {
            @Override
            public Service createReserved(String professionalId, String clientId) {
                return createReserved(professionalId, clientId, null, null);
            }

            @Override
            public Service createReserved(
                    String professionalId, String clientId, Double destinationLatitude, Double destinationLongitude) {
                return transactionTemplate.execute(
                        status -> inner.createReserved(professionalId, clientId, destinationLatitude, destinationLongitude));
            }
        };
    }

    @Bean
    GetServiceUseCase getServiceUseCase(ServiceRepositoryPort repository) {
        return new GetServiceService(repository);
    }

    @Bean
    EtaCalculator etaCalculator(@Value("${alamano.tracking.average-speed-kmh:25}") double averageSpeedKmh) {
        return new EtaCalculator(averageSpeedKmh);
    }

    @Bean
    UpdateTrackingUseCase updateTrackingUseCase(
            ServiceRepositoryPort serviceRepository,
            TrackingRepositoryPort trackingRepository,
            TrackingEventPublisherPort publisher,
            EtaCalculator etaCalculator) {
        return new UpdateTrackingService(serviceRepository, trackingRepository, publisher, etaCalculator);
    }

    @Bean
    ReportServiceLocationUseCase reportServiceLocationUseCase(
            ServiceRepositoryPort repository, UpdateTrackingUseCase tracking) {
        return new ReportServiceLocationService(repository, tracking);
    }

    @Bean
    FindNearbyProfessionalsUseCase findNearbyProfessionalsUseCase(ProfessionalQueryPort professionalQuery) {
        return new FindNearbyProfessionalsService(professionalQuery);
    }

    @Bean
    ConnectProfessionalUseCase connectProfessionalUseCase(
            ProfessionalRepositoryPort repository, ProfessionalEventPublisherPort publisher, Clock clock) {
        return new ConnectProfessionalService(repository, publisher, clock);
    }

    @Bean
    DisconnectProfessionalUseCase disconnectProfessionalUseCase(
            ProfessionalRepositoryPort repository, ProfessionalEventPublisherPort publisher, Clock clock) {
        return new DisconnectProfessionalService(repository, publisher, clock);
    }

    @Bean
    PublishPromotionUseCase publishPromotionUseCase(
            PromotionRepositoryPort repository, PromotionCounterPort counter, Clock clock) {
        return new PublishPromotionService(repository, counter, clock);
    }

    @Bean
    GetPromotionUseCase getPromotionUseCase(PromotionRepositoryPort repository, PromotionCounterPort counter) {
        return new GetPromotionService(repository, counter);
    }

    @Bean
    ListPromotionsUseCase listPromotionsUseCase(PromotionRepositoryPort repository, PromotionCounterPort counter) {
        return new ListPromotionsService(repository, counter);
    }

    @Bean
    ClaimPromotionUseCase claimPromotionUseCase(
            PromotionRepositoryPort repository,
            PromotionCounterPort counter,
            PromotionClaimPort claims,
            Clock clock) {
        return new ClaimPromotionService(repository, counter, claims, clock);
    }
}
