package com.pandora.coreservice.repo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Modifying;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryContractTest {

    @Test
    @DisplayName("EmployeeRepository.clearDepartmentByDepartmentId has @Modifying with flushAutomatically and clearAutomatically set to true")
    void employeeRepoClearDepartmentModifyingContract() throws NoSuchMethodException {
        Method method = EmployeeRepository.class.getMethod("clearDepartmentByDepartmentId", UUID.class);
        Modifying modifying = method.getAnnotation(Modifying.class);

        assertThat(modifying)
                .as("clearDepartmentByDepartmentId must be annotated with @Modifying")
                .isNotNull();
        assertThat(modifying.flushAutomatically())
                .as("flushAutomatically must be true to flush changes before bulk update")
                .isTrue();
        assertThat(modifying.clearAutomatically())
                .as("clearAutomatically must be true to invalidate persistence context after bulk update")
                .isTrue();
    }

    @Test
    @DisplayName("DepartmentRepository.clearManagerByManagerId has @Modifying with flushAutomatically and clearAutomatically set to true")
    void departmentRepoClearManagerModifyingContract() throws NoSuchMethodException {
        Method method = DepartmentRepository.class.getMethod("clearManagerByManagerId", UUID.class);
        Modifying modifying = method.getAnnotation(Modifying.class);

        assertThat(modifying)
                .as("clearManagerByManagerId must be annotated with @Modifying")
                .isNotNull();
        assertThat(modifying.flushAutomatically())
                .as("flushAutomatically must be true to flush changes before bulk update")
                .isTrue();
        assertThat(modifying.clearAutomatically())
                .as("clearAutomatically must be true to invalidate persistence context after bulk update")
                .isTrue();
    }
}
