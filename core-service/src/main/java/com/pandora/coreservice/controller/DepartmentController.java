package com.pandora.coreservice.controller;

import com.pandora.coreservice.dto.ApiResponse;
import com.pandora.coreservice.dto.DepartmentCreateRequest;
import com.pandora.coreservice.dto.DepartmentResponse;
import com.pandora.coreservice.dto.DepartmentUpdateRequest;
import com.pandora.coreservice.dto.PageResponse;
import com.pandora.coreservice.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Validated
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentResponse>> create(@Valid @RequestBody DepartmentCreateRequest request) {
        DepartmentResponse response = departmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ApiResponse<PageResponse<DepartmentResponse>> getPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(departmentService.getPaginated(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<DepartmentResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(departmentService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<DepartmentResponse> update(@PathVariable UUID id, @Valid @RequestBody DepartmentUpdateRequest request) {
        return ApiResponse.success(departmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        departmentService.delete(id);
        return ApiResponse.success(null);
    }
}
