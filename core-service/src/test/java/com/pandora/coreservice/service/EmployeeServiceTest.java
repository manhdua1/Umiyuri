package com.pandora.coreservice.service;

import com.pandora.coreservice.dto.EmployeeCreateRequest;
import com.pandora.coreservice.dto.EmployeeResponse;
import com.pandora.coreservice.dto.EmployeeUpdateRequest;
import com.pandora.coreservice.dto.PageResponse;
import com.pandora.coreservice.entity.Department;
import com.pandora.coreservice.entity.Employee;
import com.pandora.coreservice.entity.EmployeeStatus;
import com.pandora.coreservice.exception.AppException;
import com.pandora.coreservice.exception.ErrorCode;
import com.pandora.coreservice.repo.DepartmentRepository;
import com.pandora.coreservice.repo.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private UUID employeeId;
    private UUID departmentId;
    private Department activeDepartment;
    private Employee activeEmployee;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        departmentId = UUID.randomUUID();

        activeDepartment = new Department();
        activeDepartment.setId(departmentId);
        activeDepartment.setName("Engineering");
        activeDepartment.setDeletedAt(null);

        activeEmployee = new Employee();
        activeEmployee.setId(employeeId);
        activeEmployee.setCode("EMP001");
        activeEmployee.setFullName("Alice Smith");
        activeEmployee.setEmail("alice@example.com");
        activeEmployee.setDepartment(activeDepartment);
        activeEmployee.setStatus(EmployeeStatus.ACTIVE);
        activeEmployee.setDeletedAt(null);
    }

    // --- Create Tests ---

    @Test
    @DisplayName("create succeeds with valid request, trims fields, and assigns department")
    void testCreateSuccess() {
        EmployeeCreateRequest request = EmployeeCreateRequest.builder()
                .code("  EMP001  ")
                .fullName("  Alice Smith  ")
                .email("  alice@example.com  ")
                .departmentId(departmentId)
                .status(EmployeeStatus.ACTIVE)
                .build();

        when(employeeRepository.existsByCode("EMP001")).thenReturn(false);
        when(employeeRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(departmentRepository.findActiveByIdForReference(departmentId)).thenReturn(Optional.of(activeDepartment));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            emp.setId(employeeId);
            return emp;
        });

        EmployeeResponse response = employeeService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(employeeId);
        assertThat(response.getCode()).isEqualTo("EMP001");
        assertThat(response.getFullName()).isEqualTo("Alice Smith");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getDepartmentId()).isEqualTo(departmentId);
        assertThat(response.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        Employee saved = captor.getValue();
        assertThat(saved.getDeletedAt()).isNull();
        assertThat(saved.getCode()).isEqualTo("EMP001");
    }

    @Test
    @DisplayName("create defaults status to ACTIVE when status is not provided")
    void testCreateDefaultStatusActive() {
        EmployeeCreateRequest request = EmployeeCreateRequest.builder()
                .code("EMP002")
                .fullName("Bob Jones")
                .email("bob@example.com")
                .build();

        when(employeeRepository.existsByCode("EMP002")).thenReturn(false);
        when(employeeRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee emp = invocation.getArgument(0);
            emp.setId(UUID.randomUUID());
            return emp;
        });

        EmployeeResponse response = employeeService.create(request);

        assertThat(response.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(response.getDepartmentId()).isNull();
        verify(departmentRepository, never()).findActiveByIdForReference(any());
    }

    @Test
    @DisplayName("create throws INVALID_REQUEST when request is null")
    void testCreateNullRequest() {
        assertThatThrownBy(() -> employeeService.create(null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("create throws INVALID_REQUEST when required fields are missing or empty")
    void testCreateMissingRequiredFields() {
        EmployeeCreateRequest missingCode = EmployeeCreateRequest.builder()
                .code("   ")
                .fullName("Name")
                .email("test@example.com")
                .build();

        assertThatThrownBy(() -> employeeService.create(missingCode))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);

        EmployeeCreateRequest missingName = EmployeeCreateRequest.builder()
                .code("EMP003")
                .fullName("")
                .email("test@example.com")
                .build();

        assertThatThrownBy(() -> employeeService.create(missingName))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);

        EmployeeCreateRequest missingEmail = EmployeeCreateRequest.builder()
                .code("EMP003")
                .fullName("Name")
                .email(null)
                .build();

        assertThatThrownBy(() -> employeeService.create(missingEmail))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("create throws INVALID_REQUEST when field length exceeds 255 chars")
    void testCreateTooLongFields() {
        String longString = "a".repeat(256);
        EmployeeCreateRequest longCode = EmployeeCreateRequest.builder()
                .code(longString)
                .fullName("Name")
                .email("test@example.com")
                .build();

        assertThatThrownBy(() -> employeeService.create(longCode))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("create enforces global uniqueness: duplicate code throws DUPLICATE_RESOURCE")
    void testCreateDuplicateCode() {
        EmployeeCreateRequest request = EmployeeCreateRequest.builder()
                .code("EMP001")
                .fullName("Alice Smith")
                .email("alice@example.com")
                .build();

        when(employeeRepository.existsByCode("EMP001")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_RESOURCE);

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("create enforces global uniqueness: duplicate email throws DUPLICATE_RESOURCE")
    void testCreateDuplicateEmail() {
        EmployeeCreateRequest request = EmployeeCreateRequest.builder()
                .code("EMP002")
                .fullName("Alice Smith")
                .email("alice@example.com")
                .build();

        when(employeeRepository.existsByCode("EMP002")).thenReturn(false);
        when(employeeRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_RESOURCE);

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("create enforces hidden refs: soft-deleted or nonexistent department throws RESOURCE_NOT_FOUND")
    void testCreateHiddenDepartmentReference() {
        EmployeeCreateRequest request = EmployeeCreateRequest.builder()
                .code("EMP001")
                .fullName("Alice Smith")
                .email("alice@example.com")
                .departmentId(departmentId)
                .build();

        when(employeeRepository.existsByCode("EMP001")).thenReturn(false);
        when(employeeRepository.existsByEmail("alice@example.com")).thenReturn(false);
        // findActiveByIdForReference returns empty if department does not exist or has deletedAt != null
        when(departmentRepository.findActiveByIdForReference(departmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);

        verify(employeeRepository, never()).save(any());
    }

    // --- Get Paginated Tests ---

    @Test
    @DisplayName("getPaginated returns active employees and excludes soft-deleted records")
    void testGetPaginatedSuccess() {
        Pageable pageable = PageRequest.of(0, 20, org.springframework.data.domain.Sort.by("id").ascending());
        Page<Employee> page = new PageImpl<>(List.of(activeEmployee), pageable, 1);

        when(employeeRepository.findByDeletedAtIsNull(any(Pageable.class))).thenReturn(page);

        PageResponse<EmployeeResponse> result = employeeService.getPaginated(0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getContent().get(0).getCode()).isEqualTo("EMP001");
    }

    @Test
    @DisplayName("getPaginated throws INVALID_REQUEST on invalid page or size bounds")
    void testGetPaginatedInvalidBounds() {
        assertThatThrownBy(() -> employeeService.getPaginated(-1, 20))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);

        assertThatThrownBy(() -> employeeService.getPaginated(0, 0))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);

        assertThatThrownBy(() -> employeeService.getPaginated(0, 101))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }

    // --- Get By ID Tests ---

    @Test
    @DisplayName("getById returns active employee DTO")
    void testGetByIdSuccess() {
        when(employeeRepository.findByIdAndDeletedAtIsNull(employeeId)).thenReturn(Optional.of(activeEmployee));

        EmployeeResponse response = employeeService.getById(employeeId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(employeeId);
        assertThat(response.getCode()).isEqualTo("EMP001");
    }

    @Test
    @DisplayName("getById throws RESOURCE_NOT_FOUND for nonexistent or soft-deleted employee")
    void testGetByIdNotFound() {
        when(employeeRepository.findByIdAndDeletedAtIsNull(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getById(employeeId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("getById throws INVALID_REQUEST when id is null")
    void testGetByIdNullId() {
        assertThatThrownBy(() -> employeeService.getById(null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }

    // --- Update Tests ---

    @Test
    @DisplayName("update performs full replacement on active employee")
    void testUpdateSuccess() {
        UUID newDeptId = UUID.randomUUID();
        Department newDept = new Department();
        newDept.setId(newDeptId);
        newDept.setName("Design");

        EmployeeUpdateRequest request = EmployeeUpdateRequest.builder()
                .code("EMP999")
                .fullName("Alice Cooper")
                .email("cooper@example.com")
                .departmentId(newDeptId)
                .status(EmployeeStatus.INACTIVE)
                .build();

        when(employeeRepository.findActiveByIdForUpdate(employeeId)).thenReturn(Optional.of(activeEmployee));
        when(employeeRepository.existsByCodeAndIdNot("EMP999", employeeId)).thenReturn(false);
        when(employeeRepository.existsByEmailAndIdNot("cooper@example.com", employeeId)).thenReturn(false);
        when(departmentRepository.findActiveByIdForReference(newDeptId)).thenReturn(Optional.of(newDept));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArgument(0));

        EmployeeResponse response = employeeService.update(employeeId, request);

        assertThat(response.getCode()).isEqualTo("EMP999");
        assertThat(response.getFullName()).isEqualTo("Alice Cooper");
        assertThat(response.getEmail()).isEqualTo("cooper@example.com");
        assertThat(response.getDepartmentId()).isEqualTo(newDeptId);
        assertThat(response.getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
    }

    @Test
    @DisplayName("update allows clearing department with null departmentId")
    void testUpdateClearDepartment() {
        EmployeeUpdateRequest request = EmployeeUpdateRequest.builder()
                .code("EMP001")
                .fullName("Alice Smith")
                .email("alice@example.com")
                .departmentId(null)
                .status(EmployeeStatus.ACTIVE)
                .build();

        when(employeeRepository.findActiveByIdForUpdate(employeeId)).thenReturn(Optional.of(activeEmployee));
        when(employeeRepository.existsByCodeAndIdNot("EMP001", employeeId)).thenReturn(false);
        when(employeeRepository.existsByEmailAndIdNot("alice@example.com", employeeId)).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArgument(0));

        EmployeeResponse response = employeeService.update(employeeId, request);

        assertThat(response.getDepartmentId()).isNull();
        verify(departmentRepository, never()).findActiveByIdForReference(any());
    }

    @Test
    @DisplayName("update throws RESOURCE_NOT_FOUND when target employee is soft-deleted or missing")
    void testUpdateSoftDeletedEmployeeThrowsNotFound() {
        EmployeeUpdateRequest request = EmployeeUpdateRequest.builder()
                .code("EMP001")
                .fullName("Alice Smith")
                .email("alice@example.com")
                .status(EmployeeStatus.ACTIVE)
                .build();

        // findActiveByIdForUpdate excludes deletedAt IS NOT NULL
        when(employeeRepository.findActiveByIdForUpdate(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.update(employeeId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("update throws INVALID_REQUEST when status is null (status required on update)")
    void testUpdateNullStatusThrowsInvalidRequest() {
        EmployeeUpdateRequest request = EmployeeUpdateRequest.builder()
                .code("EMP001")
                .fullName("Alice Smith")
                .email("alice@example.com")
                .status(null)
                .build();

        when(employeeRepository.findActiveByIdForUpdate(employeeId)).thenReturn(Optional.of(activeEmployee));

        assertThatThrownBy(() -> employeeService.update(employeeId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("update enforces global uniqueness on code against other employees")
    void testUpdateDuplicateCodeThrowsDuplicateResource() {
        EmployeeUpdateRequest request = EmployeeUpdateRequest.builder()
                .code("EMP002")
                .fullName("Alice Smith")
                .email("alice@example.com")
                .status(EmployeeStatus.ACTIVE)
                .build();

        when(employeeRepository.findActiveByIdForUpdate(employeeId)).thenReturn(Optional.of(activeEmployee));
        when(employeeRepository.existsByCodeAndIdNot("EMP002", employeeId)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.update(employeeId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_RESOURCE);
    }

    @Test
    @DisplayName("update enforces global uniqueness on email against other employees")
    void testUpdateDuplicateEmailThrowsDuplicateResource() {
        EmployeeUpdateRequest request = EmployeeUpdateRequest.builder()
                .code("EMP001")
                .fullName("Alice Smith")
                .email("other@example.com")
                .status(EmployeeStatus.ACTIVE)
                .build();

        when(employeeRepository.findActiveByIdForUpdate(employeeId)).thenReturn(Optional.of(activeEmployee));
        when(employeeRepository.existsByCodeAndIdNot("EMP001", employeeId)).thenReturn(false);
        when(employeeRepository.existsByEmailAndIdNot("other@example.com", employeeId)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.update(employeeId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_RESOURCE);
    }

    @Test
    @DisplayName("update throws RESOURCE_NOT_FOUND when referencing soft-deleted or nonexistent department")
    void testUpdateHiddenDepartmentReferenceThrowsNotFound() {
        EmployeeUpdateRequest request = EmployeeUpdateRequest.builder()
                .code("EMP001")
                .fullName("Alice Smith")
                .email("alice@example.com")
                .departmentId(departmentId)
                .status(EmployeeStatus.ACTIVE)
                .build();

        when(employeeRepository.findActiveByIdForUpdate(employeeId)).thenReturn(Optional.of(activeEmployee));
        when(employeeRepository.existsByCodeAndIdNot("EMP001", employeeId)).thenReturn(false);
        when(employeeRepository.existsByEmailAndIdNot("alice@example.com", employeeId)).thenReturn(false);
        when(departmentRepository.findActiveByIdForReference(departmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.update(employeeId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    // --- Delete Tests ---

    @Test
    @DisplayName("delete soft-deletes employee and clears all referencing department managers")
    void testDeleteSuccessClearsManagerReferences() {
        when(employeeRepository.findActiveByIdForUpdate(employeeId)).thenReturn(Optional.of(activeEmployee));

        employeeService.delete(employeeId);

        // Verify soft delete: deletedAt was set and saved
        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        Employee saved = captor.getValue();
        assertThat(saved.getDeletedAt()).isNotNull();

        // Verify referencing department managers were detached
        verify(departmentRepository).clearManagerByManagerId(employeeId);

        // Verify no physical deletion
        verify(employeeRepository, never()).delete(any());
        verify(employeeRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete throws RESOURCE_NOT_FOUND on repeated delete (already soft-deleted or nonexistent)")
    void testRepeatedDeleteThrowsNotFound() {
        // Repeated delete: record has deletedAt != null so findActiveByIdForUpdate returns empty
        when(employeeRepository.findActiveByIdForUpdate(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.delete(employeeId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);

        verify(departmentRepository, never()).clearManagerByManagerId(any());
    }

    @Test
    @DisplayName("delete throws INVALID_REQUEST when id is null")
    void testDeleteNullIdThrowsInvalidRequest() {
        assertThatThrownBy(() -> employeeService.delete(null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }
}
