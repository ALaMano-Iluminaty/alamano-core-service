package com.alamano.core.application.service;

import com.alamano.core.application.port.in.ListActiveServicesUseCase;
import com.alamano.core.application.port.out.ServiceRepositoryPort;
import com.alamano.core.domain.service.Service;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ListActiveServicesService implements ListActiveServicesUseCase {
    private final ServiceRepositoryPort repository;

    public ListActiveServicesService(ServiceRepositoryPort repository) {
        this.repository = repository;
    }

    /**
     * Igual que GetServiceService, el usuario se identifica por su sub: se devuelven los servicios
     * activos donde participa como profesional o como cliente, del más reciente al más antiguo.
     */
    @Override
    public List<Service> listActive(String userId) {
        Map<UUID, Service> byId = new LinkedHashMap<>();
        repository.findActiveByProfessionalId(userId).forEach(service -> byId.put(service.id(), service));
        repository.findActiveByClientId(userId).forEach(service -> byId.putIfAbsent(service.id(), service));
        return byId.values().stream()
                .sorted(Comparator.comparing(Service::updatedAt).reversed())
                .toList();
    }
}
