package com.alamano.core.domain.professional;

import java.time.Instant;
import java.util.Objects;

/**
 * Vendedor (profesional). El id es el mismo sub del JWT emitido por Auth.
 * La ubicación es null si nunca se ha conectado.
 */
public record Professional(
        String id, ProfessionalStatus status, GeoPoint location, Instant locationUpdatedAt, long version) {

    public Professional {
        Objects.requireNonNull(id, "El id del vendedor es obligatorio");
        Objects.requireNonNull(status, "El estado del vendedor es obligatorio");
    }

    public static Professional connectFirstTime(String id, GeoPoint location, Instant now) {
        return new Professional(id, ProfessionalStatus.AVAILABLE, location, now, 0);
    }

    /**
     * Marca al vendedor como disponible en la nueva ubicación. Conectarse estando ya
     * disponible es válido (por ejemplo, al recargar la página) y solo actualiza la posición.
     */
    public Professional goOnline(GeoPoint location, Instant now) {
        if (status == ProfessionalStatus.BUSY) {
            throw new ProfessionalBusyException(id);
        }
        return new Professional(id, ProfessionalStatus.AVAILABLE, location, now, version + 1);
    }
}
