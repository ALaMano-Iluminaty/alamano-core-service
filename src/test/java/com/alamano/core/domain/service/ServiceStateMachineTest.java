package com.alamano.core.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ServiceStateMachineTest {
    @Test
    void reservedCanMoveToEnRoute() {
        assertEquals(ServiceStatus.EN_ROUTE, ServiceStateMachine.transition(ServiceStatus.RESERVED, ServiceStatus.EN_ROUTE));
    }

    @Test
    void completedIsTerminal() {
        assertThrows(
                InvalidServiceStatusTransitionException.class,
                () -> ServiceStateMachine.transition(ServiceStatus.COMPLETED, ServiceStatus.IN_PROGRESS));
    }

    @Test
    void cannotSkipArrived() {
        assertThrows(
                InvalidServiceStatusTransitionException.class,
                () -> ServiceStateMachine.transition(ServiceStatus.EN_ROUTE, ServiceStatus.IN_PROGRESS));
    }

    @Test
    void fullHappyPath() {
        ServiceStatus status = ServiceStatus.RESERVED;
        status = ServiceStateMachine.transition(status, ServiceStatus.EN_ROUTE);
        status = ServiceStateMachine.transition(status, ServiceStatus.ARRIVED);
        status = ServiceStateMachine.transition(status, ServiceStatus.IN_PROGRESS);
        status = ServiceStateMachine.transition(status, ServiceStatus.COMPLETED);
        assertEquals(ServiceStatus.COMPLETED, status);
    }
}
