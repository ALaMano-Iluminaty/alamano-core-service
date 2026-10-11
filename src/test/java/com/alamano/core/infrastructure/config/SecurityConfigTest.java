package com.alamano.core.infrastructure.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void serviceCreationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionalId\":\"pro-1\",\"clientId\":\"cli-1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedJwtCanCallServiceEndpoint() throws Exception {
        jdbc.update("DELETE FROM services WHERE professional_id = 'pro-sec'");
        jdbc.update("DELETE FROM professionals WHERE id = 'pro-sec'");
        jdbc.update("INSERT INTO professionals (id, status, latitude, longitude) VALUES ('pro-sec', 'AVAILABLE', 4.65, -74.06)");
        mockMvc.perform(post("/api/services")
                        .with(jwt().jwt(token -> token.subject("client-1").claim("role", "CLIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionalId\":\"pro-sec\"}"))
                .andExpect(status().isCreated());
    }
}
