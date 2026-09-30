package com.alamano.core.infrastructure.adapter.in.rest;

import com.alamano.core.domain.service.InvalidServiceStatusTransitionException;
import com.alamano.core.domain.service.ServiceNotFoundException;
import com.alamano.core.domain.service.ServiceStatusConflictException;
import com.alamano.core.domain.professional.InvalidSearchAreaException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {
    @ExceptionHandler(InvalidServiceStatusTransitionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, Object> invalidTransition(InvalidServiceStatusTransitionException ex) {
        return Map.of(
                "error", "invalid_transition",
                "message", ex.getMessage(),
                "currentStatus", ex.current().name(),
                "targetStatus", ex.target().name());
    }

    @ExceptionHandler(ServiceStatusConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, Object> statusConflict(ServiceStatusConflictException ex) {
        return Map.of(
                "error", "status_conflict",
                "message", ex.getMessage(),
                "serviceId", ex.serviceId().toString());
    }

    @ExceptionHandler(ServiceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, Object> notFound(ServiceNotFoundException ex) {
        return Map.of("error", "not_found", "message", ex.getMessage(), "serviceId", ex.serviceId().toString());
    }

    @ExceptionHandler(InvalidSearchAreaException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> invalidSearchArea(InvalidSearchAreaException ex) {
        return Map.of("error", "invalid_search_area", "message", ex.getMessage());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> invalidRequest(Exception ex) {
        return Map.of("error", "invalid_request", "message", ex.getMessage());
    }
}
