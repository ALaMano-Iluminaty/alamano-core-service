package com.alamano.core.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** HU-Q10: cada peticion lleva correlationId en MDC y en la respuesta. */
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filtro = new CorrelationIdFilter();

    @Test
    void generaUnCorrelationIdCuandoNoLlegaEnLaPeticion() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/services/active");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain cadena = new MockFilterChain();

        filtro.doFilter(request, response, cadena);

        String id = response.getHeader(CorrelationIdFilter.HEADER);
        assertThat(id).isNotBlank();
        // El filtro limpia el MDC cuando termina la peticion.
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void respetaElCorrelationIdQueLlegaEnLaCabecera() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/services/active");
        request.addHeader(CorrelationIdFilter.HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("abc-123");
    }

    @Test
    void generaUnoSiLaCabeceraLlegaEnBlanco() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/x");
        request.addHeader(CorrelationIdFilter.HEADER, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, new MockFilterChain());

        String id = response.getHeader(CorrelationIdFilter.HEADER);
        assertThat(id).isNotBlank();
        assertThat(id.trim()).isNotEmpty();
        assertThat(id).isNotEqualTo("   ");
    }
}
