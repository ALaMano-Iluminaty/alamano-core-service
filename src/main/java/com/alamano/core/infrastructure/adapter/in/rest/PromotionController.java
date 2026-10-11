package com.alamano.core.infrastructure.adapter.in.rest;

import com.alamano.core.application.port.in.ClaimPromotionUseCase;
import com.alamano.core.application.port.in.GetPromotionUseCase;
import com.alamano.core.application.port.in.ListPromotionsUseCase;
import com.alamano.core.application.port.in.PublishPromotionUseCase;
import com.alamano.core.domain.promotion.Promotion;
import com.alamano.core.domain.promotion.PromotionAvailability;
import com.alamano.core.infrastructure.adapter.in.rest.dto.ClaimPromotionResponse;
import com.alamano.core.infrastructure.adapter.in.rest.dto.PromotionResponse;
import com.alamano.core.infrastructure.adapter.in.rest.dto.PublishPromotionRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {
    private final PublishPromotionUseCase publishPromotionUseCase;
    private final GetPromotionUseCase getPromotionUseCase;
    private final ListPromotionsUseCase listPromotionsUseCase;
    private final ClaimPromotionUseCase claimPromotionUseCase;

    public PromotionController(
            PublishPromotionUseCase publishPromotionUseCase,
            GetPromotionUseCase getPromotionUseCase,
            ListPromotionsUseCase listPromotionsUseCase,
            ClaimPromotionUseCase claimPromotionUseCase) {
        this.publishPromotionUseCase = publishPromotionUseCase;
        this.getPromotionUseCase = getPromotionUseCase;
        this.listPromotionsUseCase = listPromotionsUseCase;
        this.claimPromotionUseCase = claimPromotionUseCase;
    }

    /** Solo el rol PROFESSIONAL (ver SecurityConfig). El vendedor sale del sub del token, nunca del cuerpo. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromotionResponse publish(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PublishPromotionRequest request) {
        Promotion promotion =
                publishPromotionUseCase.publish(jwt.getSubject(), request.description(), request.totalSlots());
        return PromotionResponse.from(new PromotionAvailability(promotion, promotion.totalSlots()));
    }

    @GetMapping("/mine")
    public List<PromotionResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return listPromotionsUseCase.mine(jwt.getSubject()).stream()
                .map(PromotionResponse::from)
                .toList();
    }

    /** Cualquier usuario autenticado puede ver una promoción y cuántos cupos le quedan. */
    @GetMapping("/{promotionId}")
    public PromotionResponse get(@PathVariable UUID promotionId) {
        return PromotionResponse.from(getPromotionUseCase.get(promotionId));
    }

    @PostMapping({"/{promotionId}/claim", "/{promotionId}/claims"})
    public ClaimPromotionResponse claim(
            @PathVariable UUID promotionId, @AuthenticationPrincipal Jwt jwt) {
        PromotionAvailability availability = claimPromotionUseCase.claim(promotionId, jwt.getSubject());
        return new ClaimPromotionResponse(availability.availableSlots(), availability.promotion().totalSlots());
    }
}
