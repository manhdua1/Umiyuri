package com.pandora.coreservice.dto;

import com.pandora.coreservice.entity.EmployeeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {

    private UUID id;
    private String code;
    private String fullName;
    private String email;
    private UUID departmentId;
    private EmployeeStatus status;
}
