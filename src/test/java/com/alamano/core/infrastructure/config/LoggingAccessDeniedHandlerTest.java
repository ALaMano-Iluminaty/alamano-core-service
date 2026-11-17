package com.alamano.core.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class LoggingAccessDeniedHandlerTest {
    private final LoggingAccessDeniedHandler handler = new LoggingAccessDeniedHandler();
    private final Logger logger = (Logger) LoggerFactory.getLogger(LoggingAccessDeniedHandler.class);
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void capturarLog() {
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void limpiar() {
        logger.detachAppender(appender);
        SecurityContextHolder.clearContext();
    }

    @Test
    void registraUsuarioMetodoYRutaYResponde403() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("cliente-7", "n/a"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest("POST", "/api/promotions"), response,
                new AccessDeniedException("denegado"));

        assertEquals(403, response.getStatus());
        assertEquals(1, appender.list.size());
        ILoggingEvent evento = appender.list.get(0);
        assertEquals(Level.WARN, evento.getLevel());
        String mensaje = evento.getFormattedMessage();
        assertTrue(mensaje.contains("acceso_denegado"));
        assertTrue(mensaje.contains("usuario=cliente-7"));
        assertTrue(mensaje.contains("metodo=POST"));
        assertTrue(mensaje.contains("ruta=/api/promotions"));
    }

    @Test
    void sinAutenticacionRegistraAnonimo() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest("GET", "/api/x"), response, new AccessDeniedException("denegado"));

        assertEquals(403, response.getStatus());
        assertTrue(appender.list.get(0).getFormattedMessage().contains("usuario=anonimo"));
    }

    @Test
    void limpiarQuitaSaltosDeLinea() {
        assertEquals("a_b_c_d", LoggingAccessDeniedHandler.limpiar("a\nb\rc\td"));
        assertEquals("", LoggingAccessDeniedHandler.limpiar(null));
    }
}
