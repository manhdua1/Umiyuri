package com.pandora.coreservice.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused Spring Boot native test verifying optional dotenv config import behavior,
 * precedence of external environment variables over imported dotenv properties,
 * and fail-closed validation in SecurityConfig without database dependencies.
 */
class EnvConfigLoadingTest {

    // 32-byte non-production test keys
    private static final String TEST_KEY = Base64.getEncoder()
            .encodeToString("test-dummy-key-for-dotenv-import".getBytes(StandardCharsets.UTF_8));
    private static final String OVERRIDE_KEY = Base64.getEncoder()
            .encodeToString("external-env-override-dummy-key-".getBytes(StandardCharsets.UTF_8));
    private static final String WEAK_KEY = Base64.getEncoder()
            .encodeToString("short-weak-key".getBytes(StandardCharsets.UTF_8));

    @TempDir
    Path tempDir;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withPropertyValues("spring.config.name=test-empty")
            .withUserConfiguration(SecurityConfig.class);

    private String toImportLocation(Path filePath) {
        return "optional:file:" + filePath.toAbsolutePath().toString().replace('\\', '/') + "[.properties]";
    }

    @Test
    @DisplayName("Imported dotenv JWT_SIGNING_KEY reaches SecurityConfig when no external env is set")
    void importedKeyReachesSecurityConfig() throws IOException {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "JWT_SIGNING_KEY=" + TEST_KEY + System.lineSeparator());

        contextRunner
                .withPropertyValues("spring.config.import=" + toImportLocation(envFile))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecretKey.class);

                    SecretKey secretKey = context.getBean(SecretKey.class);
                    assertThat(secretKey.getEncoded()).isEqualTo(Base64.getDecoder().decode(TEST_KEY));
                });
    }

    @Test
    @DisplayName("Imported dotenv jwt.signing-key alternative property reaches SecurityConfig")
    void importedPropertyAlternativeReachesSecurityConfig() throws IOException {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "jwt.signing-key=" + TEST_KEY + System.lineSeparator());

        contextRunner
                .withPropertyValues("spring.config.import=" + toImportLocation(envFile))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecretKey.class);

                    SecretKey secretKey = context.getBean(SecretKey.class);
                    assertThat(secretKey.getEncoded()).isEqualTo(Base64.getDecoder().decode(TEST_KEY));
                });
    }

    @Test
    @DisplayName("External environment variable JWT_SIGNING_KEY overrides dotenv imported property")
    void externalEnvOverridesDotenvImport() throws IOException {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "JWT_SIGNING_KEY=" + TEST_KEY + System.lineSeparator());

        contextRunner
                .withPropertyValues(
                        "spring.config.import=" + toImportLocation(envFile),
                        "JWT_SIGNING_KEY=" + OVERRIDE_KEY
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SecretKey.class);

                    SecretKey secretKey = context.getBean(SecretKey.class);
                    assertThat(secretKey.getEncoded()).isEqualTo(Base64.getDecoder().decode(OVERRIDE_KEY));
                    assertThat(secretKey.getEncoded()).isNotEqualTo(Base64.getDecoder().decode(TEST_KEY));
                });
    }

    @Test
    @DisplayName("Context fails closed when imported dotenv key is weak (< 32 bytes)")
    void importedWeakKeyFailsClosed() throws IOException {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "JWT_SIGNING_KEY=" + WEAK_KEY + System.lineSeparator());

        contextRunner
                .withPropertyValues("spring.config.import=" + toImportLocation(envFile))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage("JWT signing key is weak: key must be at least 32 decoded bytes (256 bits)");
                });
    }

    @Test
    @DisplayName("Context fails closed when imported dotenv key is invalid Base64")
    void importedInvalidBase64KeyFailsClosed() throws IOException {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "JWT_SIGNING_KEY=invalid_base64!@#$" + System.lineSeparator());

        contextRunner
                .withPropertyValues("spring.config.import=" + toImportLocation(envFile))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage("JWT signing key is invalid: key must be a valid Base64 string");
                });
    }

    @Test
    @DisplayName("Context fails closed when optional dotenv file does not exist and no key is provided")
    void missingFileFailsClosed() {
        Path nonExistentFile = tempDir.resolve(".env.nonexistent");

        contextRunner
                .withPropertyValues("spring.config.import=" + toImportLocation(nonExistentFile))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage("JWT signing key is missing: JWT_SIGNING_KEY is not configured");
                });
    }
}
