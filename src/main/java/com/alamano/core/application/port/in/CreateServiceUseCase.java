package com.alamano.core.application.port.in;

import com.alamano.core.domain.service.Service;

public interface CreateServiceUseCase {
    Service createReserved(String professionalId, String clientId);
}
