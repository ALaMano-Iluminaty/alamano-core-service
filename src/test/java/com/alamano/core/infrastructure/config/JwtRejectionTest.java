package com.alamano.core.infrastructure.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * HU-Q2: toda peticion a la API sin token, con token vencido o manipulado se rechaza con 401.
 * Los tokens se firman con un par de llaves generado para la prueba y el decodificador del
 * Core se reemplaza por uno que confia en esa llave: asi el camino que se ejercita es el real
 * (cabecera Authorization, filtro de Spring Security, validacion de firma y vigencia).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtRejectionTest {
    static final KeyPair PAIR = generatePair();
    static final KeyPair OTHER_PAIR = generatePair();

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class TestKeys {
        @Bean @Primary
        JwtDecoder testDecoder() {
            return NimbusJwtDecoder.withPublicKey((RSAPublicKey) PAIR.getPublic()).build();
        }
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        for (ResultActions intento : new ResultActions[] {
                mockMvc.perform(get("/api/services/active")),
                mockMvc.perform(get("/api/professionals/nearby?lat=4.6&lng=-74.0")),
                mockMvc.perform(post("/api/promotions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"x\",\"totalSlots\":5}")),
                mockMvc.perform(post("/api/professionals/me/online").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":4.6,\"longitude\":-74.0}"))}) {
            intento.andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
        }
    }

    @Test
    void unTokenValidoSiEntra() throws Exception {
        // Control: si esto fallara, los 401 de abajo no demostrarian nada.
        activos(token(PAIR, Instant.now().plusSeconds(60))).andExpect(status().isOk());
    }

    @Test
    void unTokenVencidoDevuelve401() throws Exception {
        // 5 minutos vencido: fuera de la tolerancia de reloj de 60 s de NimbusJwtDecoder.
        activos(token(PAIR, Instant.now().minus(Duration.ofMinutes(5))))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE,
                        org.hamcrest.Matchers.containsString("invalid_token")));
    }

    @Test
    void unTokenFirmadoConOtraLlaveDevuelve401() throws Exception {
        activos(token(OTHER_PAIR, Instant.now().plusSeconds(60))).andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenConElPayloadManipuladoDevuelve401() throws Exception {
        String valido = token(PAIR, Instant.now().plusSeconds(60));
        String[] partes = valido.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                .replace("\"sub\":\"client-1\"", "\"sub\":\"otro-usuario\"");
        String manipulado = partes[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + partes[2];

        activos(manipulado).andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenConLaFirmaRecortadaDevuelve401() throws Exception {
        String valido = token(PAIR, Instant.now().plusSeconds(60));
        activos(valido.substring(0, valido.length() - 8)).andExpect(status().isUnauthorized());
    }

    @Test
    void unaCabeceraQueNoEsBearerDevuelve401() throws Exception {
        mockMvc.perform(get("/api/services/active").header(HttpHeaders.AUTHORIZATION, "Basic dXN1YXJpbzpjbGF2ZQ=="))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/services/active").header(HttpHeaders.AUTHORIZATION, "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions activos(String token) throws Exception {
        return mockMvc.perform(get("/api/services/active").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private static String token(KeyPair keys, Instant expiry) {
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(
                new RSAKey.Builder((RSAPublicKey) keys.getPublic()).privateKey(keys.getPrivate()).build())));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("client-1")
                .claim("role", "CLIENT")
                .issuedAt(expiry.minus(Duration.ofMinutes(10)))
                .expiresAt(expiry)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
                .getTokenValue();
    }

    private static KeyPair generatePair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
