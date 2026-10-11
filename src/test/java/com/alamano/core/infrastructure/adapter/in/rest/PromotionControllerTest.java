package com.alamano.core.infrastructure.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alamano.core.infrastructure.adapter.out.cache.InMemoryPromotionCounterAdapter;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PromotionControllerTest {
    private static final String PROMOTIONS_URL = "/api/promotions";
    private static final String VALID_BODY = "{\"description\":\"2x1 en cortes\",\"totalSlots\":50}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Autowired
    private InMemoryPromotionCounterAdapter counter;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM promotion_claims");
        jdbc.update("DELETE FROM promotions");
    }

    @Test
    void professionalPublishesAPromotionAndPostgresAndCounterAgree() throws Exception {
        String body = mockMvc.perform(publish(token("pro-1", "PROFESSIONAL"), VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.professionalId").value("pro-1"))
                .andExpect(jsonPath("$.description").value("2x1 en cortes"))
                .andExpect(jsonPath("$.totalSlots").value(50))
                .andExpect(jsonPath("$.availableSlots").value(50))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID id = UUID.fromString(JsonPath.read(body, "$.id"));
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT professional_id, total_slots FROM promotions WHERE id = ?", id);
        assertEquals("pro-1", row.get("professional_id"));
        assertEquals(50, ((Number) row.get("total_slots")).intValue());
        assertEquals(50, counter.remaining(id).getAsInt());
    }

    @Test
    void clientCannotPublish() throws Exception {
        mockMvc.perform(publish(token("client-1", "CLIENT"), VALID_BODY)).andExpect(status().isForbidden());

        assertEquals(0, countPromotions());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post(PROMOTIONS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void zeroSlotsAreRejected() throws Exception {
        mockMvc.perform(publish(token("pro-1", "PROFESSIONAL"), "{\"description\":\"x\",\"totalSlots\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));

        assertEquals(0, countPromotions());
    }

    @Test
    void missingSlotsAreRejected() throws Exception {
        mockMvc.perform(publish(token("pro-1", "PROFESSIONAL"), "{\"description\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    @Test
    void blankDescriptionIsRejected() throws Exception {
        mockMvc.perform(publish(token("pro-1", "PROFESSIONAL"), "{\"description\":\"  \",\"totalSlots\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));

        assertEquals(0, countPromotions());
    }

    @Test
    void anyAuthenticatedUserCanSeeThePromotionAndItsSlots() throws Exception {
        String body = mockMvc.perform(publish(token("pro-1", "PROFESSIONAL"), VALID_BODY))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mockMvc.perform(get(PROMOTIONS_URL + "/" + id).with(token("client-1", "CLIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.availableSlots").value(50));
    }

    @Test
    void unknownPromotionIsNotFound() throws Exception {
        mockMvc.perform(get(PROMOTIONS_URL + "/" + UUID.randomUUID()).with(token("client-1", "CLIENT")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("promotion_not_found"));
    }

    private int countPromotions() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM promotions", Integer.class);
    }

    private MockHttpServletRequestBuilder publish(RequestPostProcessor token, String body) {
        return post(PROMOTIONS_URL).with(token).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    /** Los permisos salen del convertidor real de SecurityConfig (claim role → ROLE_<role>). */
    private RequestPostProcessor token(String subject, String role) {
        return jwt().jwt(token -> token.subject(subject).claim("role", role))
                .authorities(jwt -> new ArrayList<>(jwtAuthenticationConverter.convert(jwt).getAuthorities()));
    }
}
