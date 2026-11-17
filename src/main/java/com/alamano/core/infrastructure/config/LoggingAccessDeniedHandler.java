package com.alamano.core.infrastructure.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * HU-Q3: cada 403 por rol insuficiente deja una línea de log con usuario, método y ruta.
 * La respuesta sigue siendo la estándar del resource server (403 con WWW-Authenticate).
 * El correlationId lo agrega el MDC, que CorrelationIdFilter llena antes de la cadena de seguridad.
 */
public class LoggingAccessDeniedHandler implements AccessDeniedHandler {
    private static final Logger log = LoggerFactory.getLogger(LoggingAccessDeniedHandler.class);

    private final AccessDeniedHandler delegate = new BearerTokenAccessDeniedHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String usuario = authentication == null ? "anonimo" : limpiar(authentication.getName());
        log.warn("acceso_denegado usuario={} metodo={} ruta={} motivo=rol_insuficiente",
                usuario, limpiar(request.getMethod()), limpiar(request.getRequestURI()));
        delegate.handle(request, response, exception);
    }

    /** Evita que un valor con saltos de línea falsifique entradas del log. */
    static String limpiar(String valor) {
        return valor == null ? "" : valor.replaceAll("[\\r\\n\\t]", "_");
    }
}
