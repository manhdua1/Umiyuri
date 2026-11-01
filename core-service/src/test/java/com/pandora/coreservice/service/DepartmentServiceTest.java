package com.pandora.coreservice.service;

import com.pandora.coreservice.dto.DepartmentCreateRequest;
import com.pandora.coreservice.dto.DepartmentResponse;
import com.pandora.coreservice.dto.DepartmentUpdateRequest;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private DepartmentService departmentService;

    private UUID departmentId;
    private UUID managerId;
    private Employee activeManager;
    private Department activeDepartment;

    @BeforeEach
    void setUp() {
        departmentId = UUID.randomUUID();
        managerId = UUID.randomUUID();

        activeManager = new Employee();
        activeManager.setId(managerId);
        activeManager.setCode("MGR001");
        activeManager.setFullName("Manager Bob");
        activeManager.setEmail("bob@example.com");
        activeManager.setStatus(EmployeeStatus.ACTIVE);
        activeManager.setDeletedAt(null);

        activeDepartment = new Department();
        activeDepartment.setId(departmentId);
        activeDepartment.setName("Engineering");
        activeDepartment.setManager(activeManager);
        activeDepartment.setDeletedAt(null);
    }

    // --- Create Tests ---

    @Test
    @DisplayName("create succeeds with valid request and assigns active manager")
    void testCreateSuccessWithManager() {
        DepartmentCreateRequest request = DepartmentCreateRequest.builder()
                .name("  Product Engineering  ")
                .managerId(managerId)
                .build();

        when(employeeRepository.findActiveByIdForReference(managerId)).thenReturn(Optional.of(activeManager));
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department dept = invocation.getArgument(0);
            dept.setId(departmentId);
            return dept;
        });

        DepartmentResponse response = departmentService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(departmentId);
        assertThat(response.getName()).isEqualTo("Product Engineering");
        assertThat(response.getManagerId()).isEqualTo(managerId);

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(captor.capture());
        Department saved = captor.getValue();
        assertThat(saved.getDeletedAt()).isNull();
        assertThat(saved.getName()).isEqualTo("Product Engineering");
    }

    @Test
    @DisplayName("create succeeds without manager when managerId is null")
    void testCreateSuccessWithoutManager() {
        DepartmentCreateRequest request = DepartmentCreateRequest.builder()
                .name("Marketing")
                .managerId(null)
                .build();

        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department dept = invocation.getArgument(0);
            dept.setId(UUID.randomUUID());
            return dept;
        });

        DepartmentResponse response = departmentService.create(request);

        assertThat(response.getName()).isEqualTo("Marketing");
        assertThat(response.getManagerId()).isNull();
        verify(employeeRepository, never()).findActiveByIdForReference(any());
    }

    @Test
    @DisplayName("create throws INVALID_REQUEST when request is null")
    void testCreateNullRequest() {
        assertThatThrownBy(() -> departmentService.create(null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("create throws INVALID_REQUEST when name is blank or missing")
    void testCreateBlankOrMissingName() {
        DepartmentCreateRequest blank = DepartmentCreateRequest.builder()
                .name("   ")
                .build();

        assertThatThrownBy(() -> departmentService.create(blank))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);

        DepartmentCreateRequest nullName = DepartmentCreateRequest.builder()
                .name(null)
                .build();

        assertThatThrownBy(() -> departmentService.create(nullName))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("create throws INVALID_REQUEST when name length exceeds 255 chars")
    void testCreateTooLongName() {
        DepartmentCreateRequest longName = DepartmentCreateRequest.builder()
                .name("D".repeat(256))
                .build();

        assertThatThrownBy(() -> departmentService.create(longName))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("create enforces hidden refs: soft-deleted or nonexistent manager throws RESOURCE_NOT_FOUND")
    void testCreateHiddenManagerReference() {
        DepartmentCreateRequest request = DepartmentCreateRequest.builder()
                .name("Operations")
                .managerId(managerId)
                .build();

        // Manager is soft-deleted or nonexistent -> findActiveByIdForReference returns empty
        when(employeeRepository.findActiveByIdForReference(managerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.create(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);

        verify(departmentRepository, never()).save(any());
    }

    // --- Get Paginated Tests ---

    @Test
    @DisplayName("getPaginated returns active departments and excludes soft-deleted records")
    void testGetPaginatedSuccess() {
        Pageable pageable = PageRequest.of(0, 20, org.springframework.data.domain.Sort.by("id").ascending());
        Page<Department> page = new PageImpl<>(List.of(activeDepartment), pageable, 1);

        when(departmentRepository.findByDeletedAtIsNull(any(Pageable.class))).thenReturn(page);

        PageResponse<DepartmentResponse> result = departmentService.getPaginated(0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Engineering");
    }

    @Test
    @DisplayName("getPaginated throws INVALID_REQUEST on invalid page or size bounds")
    void testGetPaginatedInvalidBounds() {
        assertThatThrownBy(() -> departmentService.getPaginated(-1, 20))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);

        assertThatThrownBy(() -> departmentService.getPaginated(0, 0))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);

        assertThatThrownBy(() -> departmentService.getPaginated(0, 101))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }

    // --- Get By ID Tests ---

    @Test
    @DisplayName("getById returns active department DTO")
    void testGetByIdSuccess() {
        when(departmentRepository.findByIdAndDeletedAtIsNull(departmentId)).thenReturn(Optional.of(activeDepartment));

        DepartmentResponse response = departmentService.getById(departmentId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(departmentId);
        assertThat(response.getName()).isEqualTo("Engineering");
        assertThat(response.getManagerId()).isEqualTo(managerId);
    }

    @Test
    @DisplayName("getById throws RESOURCE_NOT_FOUND for nonexistent or soft-deleted department")
    void testGetByIdNotFound() {
        when(departmentRepository.findByIdAndDeletedAtIsNull(departmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getById(departmentId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("getById throws INVALID_REQUEST when id is null")
    void testGetByIdNullId() {
        assertThatThrownBy(() -> departmentService.getById(null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }

    // --- Update Tests ---

    @Test
    @DisplayName("update performs full replacement on active department and updates manager")
    void testUpdateSuccess() {
        UUID newManagerId = UUID.randomUUID();
        Employee newManager = new Employee();
        newManager.setId(newManagerId);
        newManager.setCode("MGR002");
        newManager.setFullName("Manager Carol");
        newManager.setEmail("carol@example.com");
        newManager.setStatus(EmployeeStatus.ACTIVE);

        DepartmentUpdateRequest request = DepartmentUpdateRequest.builder()
                .name("Research & Development")
                .managerId(newManagerId)
                .build();

        when(departmentRepository.findActiveByIdForUpdate(departmentId)).thenReturn(Optional.of(activeDepartment));
        when(employeeRepository.findActiveByIdForReference(newManagerId)).thenReturn(Optional.of(newManager));
        when(departmentRepository.save(any(Department.class))).thenAnswer(i -> i.getArgument(0));

        DepartmentResponse response = departmentService.update(departmentId, request);

        assertThat(response.getName()).isEqualTo("Research & Development");
        assertThat(response.getManagerId()).isEqualTo(newManagerId);
    }

    @Test
    @DisplayName("update allows clearing manager by passing null managerId")
    void testUpdateClearManager() {
        DepartmentUpdateRequest request = DepartmentUpdateRequest.builder()
                .name("Engineering")
                .managerId(null)
                .build();

        when(departmentRepository.findActiveByIdForUpdate(departmentId)).thenReturn(Optional.of(activeDepartment));
        when(departmentRepository.save(any(Department.class))).thenAnswer(i -> i.getArgument(0));

        DepartmentResponse response = departmentService.update(departmentId, request);

        assertThat(response.getManagerId()).isNull();
        verify(employeeRepository, never()).findActiveByIdForReference(any());
    }

    @Test
    @DisplayName("update throws RESOURCE_NOT_FOUND when target department is soft-deleted or missing")
    void testUpdateSoftDeletedDepartmentThrowsNotFound() {
        DepartmentUpdateRequest request = DepartmentUpdateRequest.builder()
                .name("Engineering")
                .managerId(null)
                .build();

        when(departmentRepository.findActiveByIdForUpdate(departmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.update(departmentId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("update throws INVALID_REQUEST when name is blank")
    void testUpdateBlankNameThrowsInvalidRequest() {
        DepartmentUpdateRequest request = DepartmentUpdateRequest.builder()
                .name("   ")
                .build();

        when(departmentRepository.findActiveByIdForUpdate(departmentId)).thenReturn(Optional.of(activeDepartment));

        assertThatThrownBy(() -> departmentService.update(departmentId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("update throws RESOURCE_NOT_FOUND when assigning soft-deleted or nonexistent manager")
    void testUpdateHiddenManagerReferenceThrowsNotFound() {
        DepartmentUpdateRequest request = DepartmentUpdateRequest.builder()
                .name("Engineering")
                .managerId(managerId)
                .build();

        when(departmentRepository.findActiveByIdForUpdate(departmentId)).thenReturn(Optional.of(activeDepartment));
        when(employeeRepository.findActiveByIdForReference(managerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.update(departmentId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    // --- Delete Tests ---

    @Test
    @DisplayName("delete soft-deletes department and detaches all referencing employees")
    void testDeleteSuccessDetachesEmployees() {
        when(departmentRepository.findActiveByIdForUpdate(departmentId)).thenReturn(Optional.of(activeDepartment));

        departmentService.delete(departmentId);

        // Verify soft delete
        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(captor.capture());
        Department saved = captor.getValue();
        assertThat(saved.getDeletedAt()).isNotNull();

        // Verify referencing employees were detached
        verify(employeeRepository).clearDepartmentByDepartmentId(departmentId);

        // Verify no physical deletion
        verify(departmentRepository, never()).delete(any());
        verify(departmentRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete throws RESOURCE_NOT_FOUND on repeated delete (already soft-deleted or nonexistent)")
    void testRepeatedDeleteThrowsNotFound() {
        when(departmentRepository.findActiveByIdForUpdate(departmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.delete(departmentId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);

        verify(employeeRepository, never()).clearDepartmentByDepartmentId(any());
    }

    @Test
    @DisplayName("delete throws INVALID_REQUEST when id is null")
    void testDeleteNullIdThrowsInvalidRequest() {
        assertThatThrownBy(() -> departmentService.delete(null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }
}
