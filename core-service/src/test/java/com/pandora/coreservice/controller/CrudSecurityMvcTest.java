package com.pandora.coreservice.controller;

import com.pandora.coreservice.config.SecurityConfig;
import com.pandora.coreservice.dto.DepartmentCreateRequest;
import com.pandora.coreservice.dto.DepartmentResponse;
import com.pandora.coreservice.dto.DepartmentUpdateRequest;
import com.pandora.coreservice.dto.EmployeeCreateRequest;
import com.pandora.coreservice.dto.EmployeeResponse;
import com.pandora.coreservice.dto.EmployeeUpdateRequest;
import com.pandora.coreservice.dto.PageResponse;
import com.pandora.coreservice.entity.EmployeeStatus;
import com.pandora.coreservice.entity.Role;
import com.pandora.coreservice.entity.User;
import com.pandora.coreservice.exception.GlobalExceptionHandler;
import com.pandora.coreservice.security.JwtService;
import com.pandora.coreservice.service.DepartmentService;
import com.pandora.coreservice.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {EmployeeController.class, DepartmentController.class})
@Import({SecurityConfig.class, JwtService.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "JWT_SIGNING_KEY=" + CrudSecurityMvcTest.TEST_SIGNING_KEY_BASE64)
class CrudSecurityMvcTest {

    public static final String TEST_SIGNING_KEY_BASE64 = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private DepartmentService departmentService;

    private String adminToken;
    private String managerToken;
    private String employeeToken;
    private String legacyRolelessToken;

    private final UUID sampleId = UUID.randomUUID();

    @BeforeEach
    void setUpTokens() {
        User admin = new User(UUID.randomUUID(), "admin_user", "hash", Role.Admin, true);
        User manager = new User(UUID.randomUUID(), "manager_user", "hash", Role.Manager, true);
        User employee = new User(UUID.randomUUID(), "employee_user", "hash", Role.Employee, true);

        adminToken = jwtService.generateToken(admin);
        managerToken = jwtService.generateToken(manager);
        employeeToken = jwtService.generateToken(employee);

        Instant now = Instant.now();
        JwtClaimsSet legacyClaims = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        legacyRolelessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, legacyClaims)).getTokenValue();
    }

    private static String validEmployeeJson() {
        return "{\"code\":\"EMP001\",\"fullName\":\"Jane Doe\",\"email\":\"jane@example.com\",\"status\":\"ACTIVE\"}";
    }

    private static String validDepartmentJson() {
        return "{\"name\":\"Engineering\"}";
    }

    // --- Anonymous Access (401 UNAUTHENTICATED) ---

    @Test
    @DisplayName("Anonymous access to Employee endpoints returns 401 UNAUTHENTICATED")
    void testEmployeeEndpointsAnonymous() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo(1002)));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEmployeeJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo(1002)));

        mockMvc.perform(get("/api/employees/" + sampleId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo(1002)));

        mockMvc.perform(put("/api/employees/" + sampleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEmployeeJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo(1002)));

        mockMvc.perform(delete("/api/employees/" + sampleId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo(1002)));
    }

    @Test
    @DisplayName("Anonymous access to Department endpoints returns 401 UNAUTHENTICATED")
    void testDepartmentEndpointsAnonymous() throws Exception {
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo(1002)));

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDepartmentJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo(1002)));

        mockMvc.perform(get("/api/departments/" + sampleId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo(1002)));

        mockMvc.perform(put("/api/departments/" + sampleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDepartmentJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo(1002)));

        mockMvc.perform(delete("/api/departments/" + sampleId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo(1002)));
    }

    // --- Role.Employee Access (403 ACCESS_DENIED) ---

    @Test
    @DisplayName("Role.Employee access to Employee endpoints returns 403 ACCESS_DENIED")
    void testEmployeeEndpointsForbiddenForEmployeeRole() throws Exception {
        mockMvc.perform(get("/api/employees")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(1003)));

        mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEmployeeJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(1003)));

        mockMvc.perform(delete("/api/employees/" + sampleId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(1003)));
    }

    @Test
    @DisplayName("Role.Employee access to Department endpoints returns 403 ACCESS_DENIED")
    void testDepartmentEndpointsForbiddenForEmployeeRole() throws Exception {
        mockMvc.perform(get("/api/departments")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(1003)));

        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDepartmentJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(1003)));

        mockMvc.perform(delete("/api/departments/" + sampleId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(1003)));
    }

    // --- Role.Manager Access (403 ACCESS_DENIED) ---

    @Test
    @DisplayName("Role.Manager access to Employee endpoints returns 403 ACCESS_DENIED")
    void testEmployeeEndpointsForbiddenForManagerRole() throws Exception {
        mockMvc.perform(get("/api/employees")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(1003)));

        mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEmployeeJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(1003)));

        mockMvc.perform(delete("/api/employees/" + sampleId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(1003)));
    }

    @Test
    @DisplayName("Role.Manager access to Department endpoints returns 403 ACCESS_DENIED")
    void testDepartmentEndpointsForbiddenForManagerRole() throws Exception {
        mockMvc.perform(get("/api/departments")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(1003)));

        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDepartmentJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(1003)));

        mockMvc.perform(delete("/api/departments/" + sampleId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(1003)));
    }

    // --- Legacy Roleless Token Access (403 ACCESS_DENIED) ---

    @Test
    @DisplayName("Legacy roleless token without role claim returns 403 ACCESS_DENIED")
    void testLegacyRolelessTokenForbidden() throws Exception {
        mockMvc.perform(get("/api/employees")
                        .header("Authorization", "Bearer " + legacyRolelessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(1003)));

        mockMvc.perform(get("/api/departments")
                        .header("Authorization", "Bearer " + legacyRolelessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(1003)));
    }

    // --- Role.Admin Access (200 / 201 SUCCESS) ---

    @Test
    @DisplayName("Role.Admin access to Employee endpoints succeeds")
    void testEmployeeEndpointsAdminSuccess() throws Exception {
        EmployeeResponse employeeResponse = EmployeeResponse.builder()
                .id(sampleId)
                .code("EMP001")
                .fullName("Jane Doe")
                .email("jane@example.com")
                .status(EmployeeStatus.ACTIVE)
                .build();

        PageResponse<EmployeeResponse> pageResponse = PageResponse.<EmployeeResponse>builder()
                .content(List.of(employeeResponse))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(employeeService.getPaginated(anyInt(), anyInt())).thenReturn(pageResponse);
        when(employeeService.create(any(EmployeeCreateRequest.class))).thenReturn(employeeResponse);
        when(employeeService.getById(eq(sampleId))).thenReturn(employeeResponse);
        when(employeeService.update(eq(sampleId), any(EmployeeUpdateRequest.class))).thenReturn(employeeResponse);
        doNothing().when(employeeService).delete(eq(sampleId));

        // GET paginated
        mockMvc.perform(get("/api/employees")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(1000)))
                .andExpect(jsonPath("$.result.content[0].code", equalTo("EMP001")));

        // POST create -> 201
        mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEmployeeJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", equalTo(1000)))
                .andExpect(jsonPath("$.result.code", equalTo("EMP001")));

        // GET by ID
        mockMvc.perform(get("/api/employees/" + sampleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(1000)))
                .andExpect(jsonPath("$.result.id", equalTo(sampleId.toString())));

        // PUT update
        mockMvc.perform(put("/api/employees/" + sampleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEmployeeJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(1000)));

        // DELETE
        mockMvc.perform(delete("/api/employees/" + sampleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(1000)));
    }

    @Test
    @DisplayName("Role.Admin access to Department endpoints succeeds")
    void testDepartmentEndpointsAdminSuccess() throws Exception {
        DepartmentResponse departmentResponse = DepartmentResponse.builder()
                .id(sampleId)
                .name("Engineering")
                .build();

        PageResponse<DepartmentResponse> pageResponse = PageResponse.<DepartmentResponse>builder()
                .content(List.of(departmentResponse))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(departmentService.getPaginated(anyInt(), anyInt())).thenReturn(pageResponse);
        when(departmentService.create(any(DepartmentCreateRequest.class))).thenReturn(departmentResponse);
        when(departmentService.getById(eq(sampleId))).thenReturn(departmentResponse);
        when(departmentService.update(eq(sampleId), any(DepartmentUpdateRequest.class))).thenReturn(departmentResponse);
        doNothing().when(departmentService).delete(eq(sampleId));

        // GET paginated
        mockMvc.perform(get("/api/departments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(1000)))
                .andExpect(jsonPath("$.result.content[0].name", equalTo("Engineering")));

        // POST create -> 201
        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDepartmentJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", equalTo(1000)))
                .andExpect(jsonPath("$.result.name", equalTo("Engineering")));

        // GET by ID
        mockMvc.perform(get("/api/departments/" + sampleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(1000)))
                .andExpect(jsonPath("$.result.id", equalTo(sampleId.toString())));

        // PUT update
        mockMvc.perform(put("/api/departments/" + sampleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDepartmentJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(1000)));

        // DELETE
        mockMvc.perform(delete("/api/departments/" + sampleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(1000)));
    }
}
