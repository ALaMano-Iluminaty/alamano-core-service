package com.alamano.core.application.port.in;

import com.alamano.core.domain.service.Service;
import java.util.List;

public interface ListActiveServicesUseCase {
    List<Service> listActive(String userId);
}
