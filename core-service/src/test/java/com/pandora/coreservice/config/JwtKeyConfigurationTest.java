package com.pandora.coreservice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtKeyConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SecurityConfig.class);

    private static final String VALID_KEY_BASE64 = Base64.getEncoder()
            .encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

    private static final String WEAK_KEY_BASE64 = Base64.getEncoder()
            .encodeToString("0123456789012345".getBytes(StandardCharsets.UTF_8));

    @Test
    @DisplayName("Startup fails when JWT_SIGNING_KEY is missing")
    void startupFailsWhenKeyMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage("JWT signing key is missing: JWT_SIGNING_KEY is not configured");
        });
    }

    @Test
    @DisplayName("Startup fails when JWT_SIGNING_KEY is empty or blank")
    void startupFailsWhenKeyBlank() {
        contextRunner
                .withPropertyValues("JWT_SIGNING_KEY=   ")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage("JWT signing key is missing: JWT_SIGNING_KEY is not configured");
                });
    }

    @Test
    @DisplayName("Startup fails when JWT_SIGNING_KEY is not valid Base64")
    void startupFailsWhenKeyInvalidBase64() {
        contextRunner
                .withPropertyValues("JWT_SIGNING_KEY=not-valid-base64!@#$%^")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage("JWT signing key is invalid: key must be a valid Base64 string");
                });
    }

    @Test
    @DisplayName("Startup fails when decoded key is less than 32 bytes (weak key)")
    void startupFailsWhenKeyTooShort() {
        contextRunner
                .withPropertyValues("JWT_SIGNING_KEY=" + WEAK_KEY_BASE64)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage("JWT signing key is weak: key must be at least 32 decoded bytes (256 bits)");
                });
    }

    @Test
    @DisplayName("Startup succeeds when JWT_SIGNING_KEY is valid Base64 and >= 32 decoded bytes")
    void startupSucceedsWithValidKey() {
        contextRunner
                .withPropertyValues("JWT_SIGNING_KEY=" + VALID_KEY_BASE64)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecretKey.class);
                    assertThat(context).hasSingleBean(JwtEncoder.class);
                    assertThat(context).hasSingleBean(JwtDecoder.class);

                    SecretKey secretKey = context.getBean(SecretKey.class);
                    assertThat(secretKey.getAlgorithm()).isEqualTo("HmacSHA256");
                    assertThat(secretKey.getEncoded()).hasSize(32);
                });
    }

    @Test
    @DisplayName("Startup succeeds with jwt.signing-key property alternative")
    void startupSucceedsWithPropertyAlternative() {
        contextRunner
                .withPropertyValues("jwt.signing-key=" + VALID_KEY_BASE64)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecretKey.class);
                });
    }
}
