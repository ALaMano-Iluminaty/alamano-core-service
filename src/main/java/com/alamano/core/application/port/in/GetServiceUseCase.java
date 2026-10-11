package com.alamano.core.application.port.in;

import com.alamano.core.domain.service.Service;
import java.util.UUID;

public interface GetServiceUseCase {
    Service get(UUID serviceId, String userId);
}
