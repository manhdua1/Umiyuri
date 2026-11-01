package com.pandora.coreservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmployeeTest {

    @Test
    @DisplayName("Defaults must match approved specification: status=ACTIVE, others null")
    void testDefaults() {
        Employee employee = new Employee();

        assertNull(employee.getId());
        assertNull(employee.getCode());
        assertNull(employee.getFullName());
        assertNull(employee.getEmail());
        assertNull(employee.getDepartment());
        assertEquals(EmployeeStatus.ACTIVE, employee.getStatus());
        assertNull(employee.getDeletedAt());
    }

    @Test
    @DisplayName("Accessors and mutators work correctly for all fields")
    void testAccessorsAndMutators() {
        Employee employee = new Employee();
        UUID id = UUID.randomUUID();
        Department department = new Department();
        Instant deletedAt = Instant.now();

        employee.setId(id);
        employee.setCode("EMP001");
        employee.setFullName("John Doe");
        employee.setEmail("john.doe@example.com");
        employee.setDepartment(department);
        employee.setStatus(EmployeeStatus.INACTIVE);
        employee.setDeletedAt(deletedAt);

        assertEquals(id, employee.getId());
        assertEquals("EMP001", employee.getCode());
        assertEquals("John Doe", employee.getFullName());
        assertEquals("john.doe@example.com", employee.getEmail());
        assertEquals(department, employee.getDepartment());
        assertEquals(EmployeeStatus.INACTIVE, employee.getStatus());
        assertEquals(deletedAt, employee.getDeletedAt());
    }

    @Test
    @DisplayName("Constructors initialize defaults and provided arguments properly")
    void testConstructors() {
        UUID id = UUID.randomUUID();
        Department dept = new Department();
        Instant deletedAt = Instant.now();

        Employee emp1 = new Employee(id, "EMP002", "Jane Smith", "jane.smith@example.com", dept, EmployeeStatus.RESIGNED, deletedAt);
        assertEquals(id, emp1.getId());
        assertEquals("EMP002", emp1.getCode());
        assertEquals("Jane Smith", emp1.getFullName());
        assertEquals("jane.smith@example.com", emp1.getEmail());
        assertEquals(dept, emp1.getDepartment());
        assertEquals(EmployeeStatus.RESIGNED, emp1.getStatus());
        assertEquals(deletedAt, emp1.getDeletedAt());

        Employee emp2 = new Employee();
        assertEquals(EmployeeStatus.ACTIVE, emp2.getStatus());
        assertNull(emp2.getId());
    }

    @Test
    @DisplayName("EmployeeStatus enum contains exact required constants: ACTIVE, INACTIVE, RESIGNED")
    void testEmployeeStatusEnum() {
        EmployeeStatus[] statuses = EmployeeStatus.values();
        assertEquals(3, statuses.length);
        assertArrayEquals(new EmployeeStatus[]{EmployeeStatus.ACTIVE, EmployeeStatus.INACTIVE, EmployeeStatus.RESIGNED}, statuses);
        assertEquals(EmployeeStatus.ACTIVE, EmployeeStatus.valueOf("ACTIVE"));
        assertEquals(EmployeeStatus.INACTIVE, EmployeeStatus.valueOf("INACTIVE"));
        assertEquals(EmployeeStatus.RESIGNED, EmployeeStatus.valueOf("RESIGNED"));
    }

    @Test
    @DisplayName("Entity and table mappings match JPA specification")
    void testEntityAndTableAnnotations() {
        assertTrue(Employee.class.isAnnotationPresent(Entity.class));
        Table table = Employee.class.getAnnotation(Table.class);
        assertNotNull(table);
        assertEquals("employees", table.name());
    }

    @Test
    @DisplayName("Field mappings match UUID generation, column names, constraints, and types")
    void testFieldAnnotations() throws NoSuchFieldException {
        Field idField = Employee.class.getDeclaredField("id");
        assertEquals(UUID.class, idField.getType());
        assertTrue(idField.isAnnotationPresent(Id.class));
        GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
        assertNotNull(generatedValue);
        assertEquals(GenerationType.UUID, generatedValue.strategy());
        Column idColumn = idField.getAnnotation(Column.class);
        assertNotNull(idColumn);
        assertEquals("id", idColumn.name());
        assertFalse(idColumn.nullable());
        assertFalse(idColumn.updatable());

        Field codeField = Employee.class.getDeclaredField("code");
        assertEquals(String.class, codeField.getType());
        Column codeColumn = codeField.getAnnotation(Column.class);
        assertNotNull(codeColumn);
        assertEquals("code", codeColumn.name());
        assertFalse(codeColumn.nullable());
        assertTrue(codeColumn.unique());

        Field fullNameField = Employee.class.getDeclaredField("fullName");
        assertEquals(String.class, fullNameField.getType());
        Column fullNameColumn = fullNameField.getAnnotation(Column.class);
        assertNotNull(fullNameColumn);
        assertEquals("full_name", fullNameColumn.name());
        assertFalse(fullNameColumn.nullable());

        Field emailField = Employee.class.getDeclaredField("email");
        assertEquals(String.class, emailField.getType());
        Column emailColumn = emailField.getAnnotation(Column.class);
        assertNotNull(emailColumn);
        assertEquals("email", emailColumn.name());
        assertFalse(emailColumn.nullable());
        assertTrue(emailColumn.unique());

        Field statusField = Employee.class.getDeclaredField("status");
        assertEquals(EmployeeStatus.class, statusField.getType());
        Enumerated enumerated = statusField.getAnnotation(Enumerated.class);
        assertNotNull(enumerated);
        assertEquals(EnumType.STRING, enumerated.value());
        Column statusColumn = statusField.getAnnotation(Column.class);
        assertNotNull(statusColumn);
        assertEquals("status", statusColumn.name());
        assertFalse(statusColumn.nullable());

        Field deletedAtField = Employee.class.getDeclaredField("deletedAt");
        assertEquals(Instant.class, deletedAtField.getType());
        Column deletedAtColumn = deletedAtField.getAnnotation(Column.class);
        assertNotNull(deletedAtColumn);
        assertEquals("deleted_at", deletedAtColumn.name());
        assertTrue(deletedAtColumn.nullable(), "deleted_at must be nullable");
    }

    @Test
    @DisplayName("Department association is lazy @ManyToOne without cascade and joined on department_id")
    void testDepartmentAssociationMetadata() throws NoSuchFieldException {
        Field departmentField = Employee.class.getDeclaredField("department");
        assertEquals(Department.class, departmentField.getType());

        ManyToOne manyToOne = departmentField.getAnnotation(ManyToOne.class);
        assertNotNull(manyToOne);
        assertEquals(FetchType.LAZY, manyToOne.fetch());
        assertEquals(0, manyToOne.cascade().length, "No cascade operations should be configured");

        JoinColumn joinColumn = departmentField.getAnnotation(JoinColumn.class);
        assertNotNull(joinColumn);
        assertEquals("department_id", joinColumn.name());
        assertTrue(joinColumn.nullable(), "department must be nullable");
    }

    @Test
    @DisplayName("Association allows null department and linking to Department instance")
    void testAssociationRuntime() {
        Employee employee = new Employee();
        assertNull(employee.getDepartment());

        Department department = new Department();
        department.setName("Human Resources");
        employee.setDepartment(department);
        assertEquals(department, employee.getDepartment());

        employee.setDepartment(null);
        assertNull(employee.getDepartment());
    }
}
