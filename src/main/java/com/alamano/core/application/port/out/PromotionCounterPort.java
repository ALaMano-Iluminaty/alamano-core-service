package com.alamano.core.application.port.out;

import java.util.OptionalInt;
import java.util.UUID;

/**
 * Contador de cupos de una promoción. HU12 solo lo inicia y lo lee; el decremento atómico
 * al tomar un cupo (HU11) se agrega aquí como un método más.
 */
public interface PromotionCounterPort {
    /** Deja el contador en el número de cupos de la promoción recién creada. */
    void initialize(UUID promotionId, int slots);

    /** Cupos que quedan, o vacío si el contador no existe. */
    OptionalInt remaining(UUID promotionId);
}
