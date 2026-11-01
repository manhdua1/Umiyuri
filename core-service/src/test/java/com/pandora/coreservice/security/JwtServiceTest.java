package com.pandora.coreservice.security;

import com.pandora.coreservice.entity.TestUserFactory;
import com.pandora.coreservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");

        JwtEncoder jwtEncoder = NimbusJwtEncoder.withSecretKey(secretKey).algorithm(MacAlgorithm.HS256).build();
        this.jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
        this.jwtService = new JwtService(jwtEncoder);
    }

    @Test
    @DisplayName("generateToken produces a valid HS256 JWT with UUID subject, iat, and exp of 1 hour")
    void testGenerateTokenValidClaims() {
        UUID userId = UUID.randomUUID();
        User user = TestUserFactory.createDefaultUser(userId, "test_user", "hashed_pw");

        Instant before = Instant.now().minusSeconds(1);
        String token = jwtService.generateToken(user);
        Instant after = Instant.now().plusSeconds(1);

        assertThat(token).isNotBlank();

        Jwt decoded = jwtDecoder.decode(token);
        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        assertThat(decoded.getHeaders().get("alg")).isEqualTo("HS256");

        Instant issuedAt = decoded.getIssuedAt();
        Instant expiresAt = decoded.getExpiresAt();

        assertThat(issuedAt).isNotNull();
        assertThat(expiresAt).isNotNull();
        assertThat(issuedAt).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);

        Duration duration = Duration.between(issuedAt, expiresAt);
        assertThat(duration.getSeconds()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("getExpiresIn returns 3600 and getTokenType returns Bearer")
    void testMetadata() {
        assertThat(jwtService.getExpiresIn()).isEqualTo(3600L);
        assertThat(jwtService.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("generateToken throws NullPointerException when user or user id is null")
    void testNullUserHandling() {
        assertThatThrownBy(() -> jwtService.generateToken(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("User cannot be null");

        User userWithoutId = new User();
        assertThatThrownBy(() -> jwtService.generateToken(userWithoutId))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("User ID cannot be null");
    }
}
