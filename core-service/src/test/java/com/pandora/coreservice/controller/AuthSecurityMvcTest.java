package com.pandora.coreservice.controller;

import com.pandora.coreservice.config.SecurityConfig;
import com.pandora.coreservice.entity.TestUserFactory;
import com.pandora.coreservice.entity.User;
import com.pandora.coreservice.exception.GlobalExceptionHandler;
import com.pandora.coreservice.repo.UserRepository;
import com.pandora.coreservice.security.JwtService;
import com.pandora.coreservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, AuthSecurityMvcTest.TestProtectedController.class})
@Import({SecurityConfig.class, AuthService.class, JwtService.class, GlobalExceptionHandler.class, AuthSecurityMvcTest.TestProtectedController.class})
@TestPropertySource(properties = "JWT_SIGNING_KEY=" + AuthSecurityMvcTest.TEST_SIGNING_KEY_BASE64)
class AuthSecurityMvcTest {

    // 32-byte dummy key Base64-encoded for isolated test pipeline
    public static final String TEST_SIGNING_KEY_BASE64 = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private UserRepository userRepository;

    private User activeUser;
    private User disabledUser;
    private final UUID activeUserId = UUID.randomUUID();
    private final UUID disabledUserId = UUID.randomUUID();
    private final String rawPassword = "correct_password_123";

    @BeforeEach
    void setupUsers() {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        activeUser = TestUserFactory.createDefaultUser(activeUserId, "alice", encodedPassword);
        disabledUser = TestUserFactory.createDisabledUser(disabledUserId, "bob_disabled", encodedPassword);
    }

    private static String loginJson(String username, String password) {
        String u = username != null ? "\"" + username + "\"" : "null";
        String p = password != null ? "\"" + password + "\"" : "null";
        return "{\"username\":" + u + ",\"password\":" + p + "}";
    }

    // --- Credential Success ---

    @Test
    @DisplayName("POST /api/auth/login succeeds with valid credentials and returns ApiResponse with code 1000, result, and no message")
    void testLoginSuccess() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(activeUser));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("alice", rawPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(1000)))
                .andExpect(jsonPath("$.result.accessToken", notNullValue()))
                .andExpect(jsonPath("$.result.tokenType", equalTo("Bearer")))
                .andExpect(jsonPath("$.result.expiresIn", equalTo(3600)))
                .andExpect(jsonPath("$.message").doesNotExist())
                .andReturn();

        // Stateless verification: session must be null
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    // --- Credential Failures (generic 401 with ErrorCode 1001) ---

    @Test
    @DisplayName("POST /api/auth/login returns generic 401 with code 1001 and no result when password does not match")
    void testLoginWrongPassword() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(activeUser));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("alice", "wrong_password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo(1001)))
                .andExpect(jsonPath("$.message", equalTo("Invalid credentials")))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/login returns generic 401 with code 1001 and no result when username is unknown")
    void testLoginUnknownUser() throws Exception {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("nonexistent", rawPassword)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo(1001)))
                .andExpect(jsonPath("$.message", equalTo("Invalid credentials")))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/login returns generic 401 with code 1001 and no result when user is disabled")
    void testLoginDisabledUser() throws Exception {
        when(userRepository.findByUsername("bob_disabled")).thenReturn(Optional.of(disabledUser));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("bob_disabled", rawPassword)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo(1001)))
                .andExpect(jsonPath("$.message", equalTo("Invalid credentials")))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    // --- Malformed Input (400 Bad Request with ErrorCode 1004) ---

    @Test
    @DisplayName("POST /api/auth/login returns 400 Bad Request with code 1004 when username is missing or blank")
    void testLoginMissingUsername() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("", rawPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1004)))
                .andExpect(jsonPath("$.message", equalTo("Malformed request")))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/login returns 400 Bad Request with code 1004 when password is missing or blank")
    void testLoginMissingPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("alice", "   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1004)))
                .andExpect(jsonPath("$.message", equalTo("Malformed request")))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/login returns 400 Bad Request with code 1004 on invalid JSON syntax")
    void testLoginMalformedJson() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"alice\", \"password\":}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(1004)))
                .andExpect(jsonPath("$.message", equalTo("Malformed request")))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    // --- Method and Path Public Restriction ---

    @Test
    @DisplayName("GET /api/auth/login without token returns 401 with standard envelope (only POST /api/auth/login is public)")
    void testMethodPublicRestrictionGetLogin() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(jsonPath("$.code", equalTo(1002)))
                .andExpect(jsonPath("$.message", equalTo("Unauthenticated")))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    @DisplayName("PUT /api/auth/login without token returns 401 with standard envelope")
    void testMethodPublicRestrictionPutLogin() throws Exception {
        mockMvc.perform(put("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("alice", "pwd")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo(1002)))
                .andExpect(jsonPath("$.message", equalTo("Unauthenticated")))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    // --- Protected Route Security ---

    @Test
    @DisplayName("GET /api/test/protected without token returns 401 Unauthorized with standard envelope")
    void testProtectedEndpointNoToken() throws Exception {
        mockMvc.perform(get("/api/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")))
                .andExpect(jsonPath("$.code", equalTo(1002)))
                .andExpect(jsonPath("$.message", equalTo("Unauthenticated")))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/test/protected with valid ****** returns 200 OK")
    void testProtectedEndpointValidToken() throws Exception {
        String token = jwtService.generateToken(activeUser);

        MvcResult result = mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("ok")))
                .andExpect(jsonPath("$.subject", equalTo(activeUserId.toString())))
                .andReturn();

        // Stateless verification: session must be null
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    @DisplayName("GET /api/test/protected with expired token returns 401 Unauthorized with standard envelope")
    void testProtectedEndpointExpiredToken() throws Exception {
        Instant now = Instant.now();
        JwtClaimsSet expiredClaims = JwtClaimsSet.builder()
                .subject(activeUserId.toString())
                .issuedAt(now.minusSeconds(7200))
                .expiresAt(now.minusSeconds(3600))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(header, expiredClaims)).getTokenValue();

        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.containsString("error=\"invalid_token\"")))
                .andExpect(jsonPath("$.code", equalTo(1002)))
                .andExpect(jsonPath("$.message", equalTo("Unauthenticated")))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/test/protected with tampered token returns 401 Unauthorized with standard envelope")
    void testProtectedEndpointTamperedToken() throws Exception {
        String validToken = jwtService.generateToken(activeUser);
        // Tamper with the signature bytes by altering the last characters
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";

        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.containsString("error=\"invalid_token\"")))
                .andExpect(jsonPath("$.code", equalTo(1002)))
                .andExpect(jsonPath("$.message", equalTo("Unauthenticated")))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/test/unexpected-error returns 500 with safe envelope and no internal details")
    void testUnexpectedErrorHandling() throws Exception {
        String token = jwtService.generateToken(activeUser);

        mockMvc.perform(get("/api/test/unexpected-error")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code", equalTo(9999)))
                .andExpect(jsonPath("$.message", equalTo("Internal server error")))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/test/denied returns 403 with access denied envelope")
    void testAccessDeniedHandling() throws Exception {
        String token = jwtService.generateToken(activeUser);

        mockMvc.perform(get("/api/test/denied")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(1003)))
                .andExpect(jsonPath("$.message", equalTo("Access denied")))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    // --- Helper Protected Controller for MVC Isolation Test ---

    @RestController
    static class TestProtectedController {

        @GetMapping("/api/test/protected")
        public Map<String, String> protectedEndpoint(org.springframework.security.core.Authentication auth) {
            return Map.of(
                    "status", "ok",
                    "subject", auth.getName()
            );
        }

        @GetMapping("/api/test/unexpected-error")
        public String unexpectedError() {
            throw new RuntimeException("Simulated unexpected internal database failure");
        }

        @GetMapping("/api/test/denied")
        public String accessDenied() {
            throw new org.springframework.security.access.AccessDeniedException("Simulated forbidden access");
        }
    }
}
