package com.alamano.core.application.port.out;

import com.alamano.core.domain.professional.Professional;
import java.util.Optional;

public interface ProfessionalRepositoryPort {
    Optional<Professional> findById(String id);

    /** Devuelve false si ya existe un vendedor con ese id. */
    boolean insert(Professional professional);

    /** Devuelve true solo si la fila seguía en la versión esperada y se actualizó. */
    boolean updateIfVersionMatches(Professional updated, long expectedVersion);
}
