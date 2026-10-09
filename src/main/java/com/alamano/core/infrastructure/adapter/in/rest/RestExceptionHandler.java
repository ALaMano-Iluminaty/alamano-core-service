package com.alamano.core.infrastructure.adapter.in.rest;

import com.alamano.core.domain.service.InvalidServiceStatusTransitionException;
import com.alamano.core.domain.service.ServiceNotFoundException;
import com.alamano.core.domain.service.ServiceAccessDeniedException;
import com.alamano.core.domain.service.ServiceStatusConflictException;
import com.alamano.core.domain.professional.InvalidSearchAreaException;
import com.alamano.core.domain.professional.ProfessionalBusyException;
import com.alamano.core.domain.professional.ProfessionalConcurrentUpdateException;
import com.alamano.core.domain.professional.ProfessionalNotFoundException;
import com.alamano.core.domain.promotion.InvalidPromotionException;
import com.alamano.core.domain.promotion.PromotionCounterUnavailableException;
import com.alamano.core.domain.promotion.PromotionNotFoundException;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {
    @ExceptionHandler(ServiceAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    Map<String, Object> accessDenied(ServiceAccessDeniedException ex) {
        return Map.of("error", "service_access_denied", "message", ex.getMessage());
    }

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> invalidBody(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Map.of("error", "invalid_request", "message", message);
    }

    @ExceptionHandler(ProfessionalBusyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, Object> professionalBusy(ProfessionalBusyException ex) {
        return Map.of("error", "professional_busy", "message", ex.getMessage());
    }

    @ExceptionHandler(ProfessionalConcurrentUpdateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, Object> professionalConcurrentUpdate(ProfessionalConcurrentUpdateException ex) {
        return Map.of("error", "concurrent_update", "message", ex.getMessage());
    }

    @ExceptionHandler(ProfessionalNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, Object> professionalNotFound(ProfessionalNotFoundException ex) {
        return Map.of("error", "professional_not_found", "message", ex.getMessage());
    }

    @ExceptionHandler(InvalidPromotionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> invalidPromotion(InvalidPromotionException ex) {
        return Map.of("error", "invalid_promotion", "message", ex.getMessage());
    }

    @ExceptionHandler(PromotionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, Object> promotionNotFound(PromotionNotFoundException ex) {
        return Map.of("error", "promotion_not_found", "message", ex.getMessage(),
                "promotionId", ex.promotionId().toString());
    }

    @ExceptionHandler(PromotionCounterUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    Map<String, Object> promotionCounterUnavailable(PromotionCounterUnavailableException ex) {
        return Map.of("error", "promotion_counter_unavailable", "message", ex.getMessage());
    }
}
