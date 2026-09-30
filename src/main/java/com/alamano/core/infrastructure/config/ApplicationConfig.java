package com.alamano.core.infrastructure.config;

import com.alamano.core.application.port.in.ChangeServiceStatusUseCase;
import com.alamano.core.application.port.in.CreateServiceUseCase;
import com.alamano.core.application.port.in.FindNearbyProfessionalsUseCase;
import com.alamano.core.application.port.out.ProfessionalQueryPort;
import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.application.port.out.ServiceStatusChangedPublisherPort;
import com.alamano.core.application.service.ChangeServiceStatusService;
import com.alamano.core.application.service.CreateServiceService;
import com.alamano.core.application.service.FindNearbyProfessionalsService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    CreateServiceUseCase createServiceUseCase(ServiceRepositoryPort repository, Clock clock) {
        return new CreateServiceService(repository, clock);
    }

    @Bean
    FindNearbyProfessionalsUseCase findNearbyProfessionalsUseCase(ProfessionalQueryPort professionalQuery) {
        return new FindNearbyProfessionalsService(professionalQuery);
    }
}
