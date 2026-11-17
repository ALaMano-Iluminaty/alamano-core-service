package com.alamano.core.infrastructure.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.alamano.core.infrastructure.config.LoggingAccessDeniedHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** HU-Q3 y HU-Q5: el 403 por rol deja log con correlationId, y el cuerpo mal formado responde 400. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuditAndValidationTest {
    @Autowired
    private MockMvc mockMvc;

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
    }

    @Test
    void clienteNoPuedePublicarPromocionesYQuedaEnElLog() throws Exception {
        mockMvc.perform(post("/api/promotions")
                        .with(jwt().jwt(token -> token.subject("cliente-9").claim("role", "CLIENT")))
                        .header("X-Correlation-Id", "corr-403")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"x\",\"totalSlots\":5}"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Correlation-Id", "corr-403"));

        assertTrue(appender.list.stream().anyMatch(evento ->
                evento.getFormattedMessage().contains("usuario=cliente-9")
                        && evento.getFormattedMessage().contains("ruta=/api/promotions")));
    }

    @Test
    void cuerpoQueNoEsJsonResponde400() throws Exception {
        mockMvc.perform(post("/api/services")
                        .with(jwt().jwt(token -> token.subject("cliente-9").claim("role", "CLIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    @Test
    void identificadorDeVendedorDemasiadoLargoResponde400() throws Exception {
        String largo = "x".repeat(65);
        mockMvc.perform(post("/api/services")
                        .with(jwt().jwt(token -> token.subject("cliente-9").claim("role", "CLIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionalId\":\"" + largo + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    @Test
    void textoDeInyeccionSqlComoVendedorSeTrataComoUnIdCualquiera() throws Exception {
        mockMvc.perform(post("/api/services")
                        .with(jwt().jwt(token -> token.subject("cliente-9").claim("role", "CLIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionalId\":\"x'; DROP TABLE services;--\"}"))
                .andExpect(status().isConflict());
    }
}
