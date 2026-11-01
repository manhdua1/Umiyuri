package com.pandora.coreservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepartmentTest {

    @Test
    @DisplayName("Defaults must match approved specification: id=null, name=null, manager=null, deletedAt=null")
    void testDefaults() {
        Department dept = new Department();
        assertNull(dept.getId());
        assertNull(dept.getName());
        assertNull(dept.getManager());
        assertNull(dept.getDeletedAt());
    }

    @Test
    @DisplayName("Accessors and mutators work correctly for all fields")
    void testAccessorsAndMutators() {
        Department dept = new Department();
        UUID id = UUID.randomUUID();
        Employee manager = new Employee();
        Instant deletedAt = Instant.now();

        dept.setId(id);
        dept.setName("Engineering");
        dept.setManager(manager);
        dept.setDeletedAt(deletedAt);

        assertEquals(id, dept.getId());
        assertEquals("Engineering", dept.getName());
        assertEquals(manager, dept.getManager());
        assertEquals(deletedAt, dept.getDeletedAt());
    }

    @Test
    @DisplayName("Constructors initialize all fields properly")
    void testConstructors() {
        UUID id = UUID.randomUUID();
        Employee manager = new Employee();
        Instant deletedAt = Instant.now();
        Department dept = new Department(id, "Finance", manager, deletedAt);

        assertEquals(id, dept.getId());
        assertEquals("Finance", dept.getName());
        assertEquals(manager, dept.getManager());
        assertEquals(deletedAt, dept.getDeletedAt());

        Department deptNullDeleted = new Department(id, "Finance", manager, null);
        assertEquals(id, deptNullDeleted.getId());
        assertNull(deptNullDeleted.getDeletedAt());

        Department emptyDept = new Department();
        assertNull(emptyDept.getId());
        assertNull(emptyDept.getName());
        assertNull(emptyDept.getManager());
        assertNull(emptyDept.getDeletedAt());
    }

    @Test
    @DisplayName("Entity and table mappings match JPA specification")
    void testEntityAndTableAnnotations() {
        assertTrue(Department.class.isAnnotationPresent(Entity.class));
        Table table = Department.class.getAnnotation(Table.class);
        assertNotNull(table);
        assertEquals("departments", table.name());
    }

    @Test
    @DisplayName("Field mappings match UUID generation, column names, constraints, and types")
    void testFieldAnnotations() throws NoSuchFieldException {
        Field idField = Department.class.getDeclaredField("id");
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

        Field nameField = Department.class.getDeclaredField("name");
        assertEquals(String.class, nameField.getType());
        Column nameColumn = nameField.getAnnotation(Column.class);
        assertNotNull(nameColumn);
        assertEquals("name", nameColumn.name());
        assertFalse(nameColumn.nullable());

        Field deletedAtField = Department.class.getDeclaredField("deletedAt");
        assertEquals(Instant.class, deletedAtField.getType());
        Column deletedAtColumn = deletedAtField.getAnnotation(Column.class);
        assertNotNull(deletedAtColumn);
        assertEquals("deleted_at", deletedAtColumn.name());
        assertTrue(deletedAtColumn.nullable(), "deleted_at must be nullable");
    }

    @Test
    @DisplayName("Manager association is lazy @ManyToOne without cascade and joined on manager_id")
    void testManagerAssociationMetadata() throws NoSuchFieldException {
        Field managerField = Department.class.getDeclaredField("manager");
        assertEquals(Employee.class, managerField.getType());

        ManyToOne manyToOne = managerField.getAnnotation(ManyToOne.class);
        assertNotNull(manyToOne);
        assertEquals(FetchType.LAZY, manyToOne.fetch());
        assertEquals(0, manyToOne.cascade().length, "No cascade operations should be configured");

        JoinColumn joinColumn = managerField.getAnnotation(JoinColumn.class);
        assertNotNull(joinColumn);
        assertEquals("manager_id", joinColumn.name());
        assertTrue(joinColumn.nullable(), "manager must be nullable");
    }

    @Test
    @DisplayName("Association allows null manager and linking to Employee instance")
    void testAssociationRuntime() {
        Department dept = new Department();
        assertNull(dept.getManager());

        Employee emp = new Employee();
        emp.setCode("MGR001");
        dept.setManager(emp);
        assertEquals(emp, dept.getManager());

        dept.setManager(null);
        assertNull(dept.getManager());
    }
}
