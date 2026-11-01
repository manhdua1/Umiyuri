package com.pandora.coreservice.controller;

import com.pandora.coreservice.config.SecurityConfig;
import com.pandora.coreservice.exception.GlobalExceptionHandler;
import com.pandora.coreservice.security.JwtService;
import com.pandora.coreservice.service.AuthService;
import com.pandora.coreservice.service.DepartmentService;
import com.pandora.coreservice.service.EmployeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, EmployeeController.class, DepartmentController.class})
@Import({SecurityConfig.class, JwtService.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "JWT_SIGNING_KEY=" + CorsSecurityMvcTest.TEST_SIGNING_KEY_BASE64)
class CorsSecurityMvcTest {

    public static final String TEST_SIGNING_KEY_BASE64 = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private DepartmentService departmentService;

    // --- 1. Allowed Origin Preflight on Public Endpoint (/api/auth/login) ---

    @ParameterizedTest(name = "Preflight /api/auth/login allowed for origin {0}")
    @ValueSource(strings = {
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost:4173",
            "http://127.0.0.1:4173"
    })
    @DisplayName("Preflight OPTIONS /api/auth/login from default allowed origins succeeds with 200 and CORS headers")
    void testAllowedLoginPreflightAllDefaultOrigins(String origin) throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, Accept"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    // --- 2. Allowed Origin Preflight on Protected Endpoints Without Auth ---

    @Test
    @DisplayName("Preflight OPTIONS /api/employees without auth succeeds with 200 and CORS headers")
    void testProtectedEmployeePreflightNoAuth() throws Exception {
        mockMvc.perform(options("/api/employees")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Authorization")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    @DisplayName("Preflight OPTIONS /api/departments without auth succeeds with 200 and CORS headers")
    void testProtectedDepartmentPreflightNoAuth() throws Exception {
        mockMvc.perform(options("/api/departments")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    @DisplayName("Preflight OPTIONS /api/employees/{id} PUT without auth succeeds with 200")
    void testProtectedEmployeeSubpathPreflightNoAuth() throws Exception {
        mockMvc.perform(options("/api/employees/00000000-0000-0000-0000-000000000001")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("PUT")));
    }

    @Test
    @DisplayName("Preflight OPTIONS /api/departments/{id} DELETE without auth succeeds with 200")
    void testProtectedDepartmentSubpathPreflightNoAuth() throws Exception {
        mockMvc.perform(options("/api/departments/00000000-0000-0000-0000-000000000001")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:4173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:4173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("DELETE")));
    }

    // --- 3. Allowed Origin Actual Requests Resulting in 401 Retain Access-Control-Allow-Origin ---

    @Test
    @DisplayName("Actual GET /api/employees without token returns 401 with Access-Control-Allow-Origin header")
    void testAllowedOriginActual401WithAllowOriginHeader() throws Exception {
        mockMvc.perform(get("/api/employees")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("Bearer")))
                .andExpect(jsonPath("$.code", equalTo(1002)))
                .andExpect(jsonPath("$.message", equalTo("Unauthenticated")));
    }

    @Test
    @DisplayName("Actual POST /api/departments without token returns 401 with Access-Control-Allow-Origin header")
    void testAllowedOriginActualPost401WithAllowOriginHeader() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173")
                        .contentType("application/json")
                        .content("{\"name\":\"Engineering\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5173"))
                .andExpect(jsonPath("$.code", equalTo(1002)))
                .andExpect(jsonPath("$.message", equalTo("Unauthenticated")));
    }

    // --- 4. Foreign Origin Rejected with 403 Forbidden and No Access-Control-Allow-Origin ---

    @Test
    @DisplayName("Preflight OPTIONS /api/auth/login from foreign origin returns 403 without Allow-Origin header")
    void testForeignOriginPreflightLoginReturns403() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://evil.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Preflight OPTIONS /api/employees from foreign origin returns 403 without Allow-Origin header")
    void testForeignOriginPreflightProtectedReturns403() throws Exception {
        mockMvc.perform(options("/api/employees")
                        .header(HttpHeaders.ORIGIN, "http://malicious.org")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Actual GET /api/employees from foreign origin returns 403 without Allow-Origin header")
    void testForeignOriginActualRequestReturns403() throws Exception {
        mockMvc.perform(get("/api/employees")
                        .header(HttpHeaders.ORIGIN, "http://evil.com"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    // --- 5. Disallowed Methods and Headers Rejected with 403 ---

    @Test
    @DisplayName("Preflight OPTIONS with disallowed method (PATCH) returns 403 without Allow-Origin header")
    void testDisallowedMethodRejected() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Preflight OPTIONS with disallowed header (X-Custom-Header) returns 403 without Allow-Origin header")
    void testDisallowedHeaderRejected() throws Exception {
        mockMvc.perform(options("/api/employees")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-Custom-Header"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    // --- 6. Direct Configuration Source Unit Verification ---

    @Test
    @DisplayName("CorsConfigurationSource properly handles /api/** and rejects paths outside /api/**")
    void testCorsConfigurationSourceDirectVerification() {
        MockHttpServletRequest apiRequest = new MockHttpServletRequest("GET", "/api/test");
        CorsConfiguration apiConfig = corsConfigurationSource.getCorsConfiguration(apiRequest);
        assertThat(apiConfig).isNotNull();
        assertThat(apiConfig.getAllowedOrigins()).containsExactlyInAnyOrder(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:4173",
                "http://127.0.0.1:4173"
        );
        assertThat(apiConfig.getAllowedMethods()).containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(apiConfig.getAllowedHeaders()).containsExactlyInAnyOrder("Authorization", "Content-Type", "Accept");
        assertThat(apiConfig.getAllowCredentials()).isFalse();
        assertThat(apiConfig.getMaxAge()).isEqualTo(3600L);

        // Path outside /api/** should not match CORS config
        MockHttpServletRequest nonApiRequest = new MockHttpServletRequest("GET", "/other/resource");
        CorsConfiguration nonApiConfig = corsConfigurationSource.getCorsConfiguration(nonApiRequest);
        assertThat(nonApiConfig).isNull();
    }

    @Test
    @DisplayName("CorsConfigurationSource correctly parses comma-delimited strings and trims whitespace")
    void testCorsConfigurationSourceParsingLogic() {
        SecurityConfig config = new SecurityConfig();
        CorsConfigurationSource source = config.corsConfigurationSource(List.of(" http://custom1.com , http://custom2.com:8080 "));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/any");
        CorsConfiguration corsConfig = source.getCorsConfiguration(request);
        assertThat(corsConfig).isNotNull();
        assertThat(corsConfig.getAllowedOrigins()).containsExactly("http://custom1.com", "http://custom2.com:8080");
    }

    // --- 7. Configurable Property Override Verification ---

    @Test
    @DisplayName("CorsConfigurationSource honors configurable property override via Spring application context")
    void testCorsConfigurationSourcePropertyOverrideViaContextRunner() {
        new org.springframework.boot.test.context.runner.WebApplicationContextRunner()
                .withUserConfiguration(SecurityConfig.class)
                .withPropertyValues(
                        "JWT_SIGNING_KEY=" + TEST_SIGNING_KEY_BASE64,
                        "app.cors.allowed-origins=http://custom-frontend.example.com,http://another.example.com:3000"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    CorsConfigurationSource source = context.getBean(CorsConfigurationSource.class);
                    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
                    CorsConfiguration config = source.getCorsConfiguration(request);
                    assertThat(config).isNotNull();
                    assertThat(config.getAllowedOrigins()).containsExactly(
                            "http://custom-frontend.example.com",
                            "http://another.example.com:3000"
                    );
                });
    }
}
