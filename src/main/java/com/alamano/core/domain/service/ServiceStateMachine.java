package com.alamano.core.domain.service;

import java.util.EnumSet;
import java.util.Set;

public final class ServiceStateMachine {

    private ServiceStateMachine() {
    }

    public static ServiceStatus transition(ServiceStatus current, ServiceStatus target) {
        if (current == target) {
            throw new InvalidServiceStatusTransitionException(current, target);
        }
        if (!allowedTargets(current).contains(target)) {
            throw new InvalidServiceStatusTransitionException(current, target);
        }
        return target;
    }

    public static Set<ServiceStatus> allowedTargets(ServiceStatus current) {
        return switch (current) {
            case RESERVED -> EnumSet.of(ServiceStatus.EN_ROUTE, ServiceStatus.CANCELLED);
            case EN_ROUTE -> EnumSet.of(ServiceStatus.ARRIVED, ServiceStatus.CANCELLED);
            case ARRIVED -> EnumSet.of(ServiceStatus.IN_PROGRESS);
            case IN_PROGRESS -> EnumSet.of(ServiceStatus.COMPLETED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(ServiceStatus.class);
        };
    }
}
