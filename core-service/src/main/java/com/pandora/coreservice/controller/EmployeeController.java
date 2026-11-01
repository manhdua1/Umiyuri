package com.pandora.coreservice.controller;

import com.pandora.coreservice.dto.ApiResponse;
import com.pandora.coreservice.dto.EmployeeCreateRequest;
import com.pandora.coreservice.dto.EmployeeResponse;
import com.pandora.coreservice.dto.EmployeeUpdateRequest;
import com.pandora.coreservice.dto.PageResponse;
import com.pandora.coreservice.service.EmployeeService;
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
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Validated
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeResponse response = employeeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ApiResponse<PageResponse<EmployeeResponse>> getPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(employeeService.getPaginated(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<EmployeeResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(employeeService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<EmployeeResponse> update(@PathVariable UUID id, @Valid @RequestBody EmployeeUpdateRequest request) {
        return ApiResponse.success(employeeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        employeeService.delete(id);
        return ApiResponse.success(null);
    }
}
