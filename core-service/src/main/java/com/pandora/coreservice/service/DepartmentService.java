package com.pandora.coreservice.service;

import com.pandora.coreservice.dto.DepartmentCreateRequest;
import com.pandora.coreservice.dto.DepartmentResponse;
import com.pandora.coreservice.dto.DepartmentUpdateRequest;
import com.pandora.coreservice.dto.PageResponse;
import com.pandora.coreservice.entity.Department;
import com.pandora.coreservice.entity.Employee;
import com.pandora.coreservice.exception.AppException;
import com.pandora.coreservice.exception.ErrorCode;
import com.pandora.coreservice.repo.DepartmentRepository;
import com.pandora.coreservice.repo.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public DepartmentResponse create(DepartmentCreateRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        String name = request.getName() != null ? request.getName().trim() : null;
        if (name == null || name.isEmpty() || name.length() > 255) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Employee manager = null;
        if (request.getManagerId() != null) {
            manager = employeeRepository.findActiveByIdForReference(request.getManagerId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        }

        Department department = new Department();
        department.setName(name);
        department.setManager(manager);
        department.setDeletedAt(null);

        Department saved = departmentRepository.save(department);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> getPaginated(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Department> deptPage = departmentRepository.findByDeletedAtIsNull(pageable);
        List<DepartmentResponse> content = deptPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<DepartmentResponse>builder()
                .content(content)
                .page(deptPage.getNumber())
                .size(deptPage.getSize())
                .totalElements(deptPage.getTotalElements())
                .totalPages(deptPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getById(UUID id) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        return toResponse(department);
    }

    @Transactional
    public DepartmentResponse update(UUID id, DepartmentUpdateRequest request) {
        if (id == null || request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        Department department = departmentRepository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        String name = request.getName() != null ? request.getName().trim() : null;
        if (name == null || name.isEmpty() || name.length() > 255) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        department.setName(name);

        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findActiveByIdForReference(request.getManagerId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
            department.setManager(manager);
        } else {
            department.setManager(null);
        }

        Department saved = departmentRepository.save(department);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (id == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        Department department = departmentRepository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        department.setDeletedAt(Instant.now());
        departmentRepository.save(department);

        employeeRepository.clearDepartmentByDepartmentId(id);
    }

    public DepartmentResponse toResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .managerId(department.getManager() != null ? department.getManager().getId() : null)
                .build();
    }
}
