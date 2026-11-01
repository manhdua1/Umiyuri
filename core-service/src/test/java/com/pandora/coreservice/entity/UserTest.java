package com.pandora.coreservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    @DisplayName("Defaults must match approved specification: role=Employee, enabled=true, id=null")
    void testDefaults() {
        User user = new User();

        assertNull(user.getId());
        assertNull(user.getUsername());
        assertNull(user.getPasswordHash());
        assertEquals(Role.Employee, user.getRole());
        assertTrue(user.isEnabled());
    }

    @Test
    @DisplayName("Accessors and mutators work correctly for all fields")
    void testAccessorsAndMutators() {
        User user = new User();
        UUID id = UUID.randomUUID();

        user.setId(id);
        user.setUsername("john_doe");
        user.setPasswordHash("hashed_pw_123");
        user.setRole(Role.Admin);
        user.setEnabled(false);

        assertEquals(id, user.getId());
        assertEquals("john_doe", user.getUsername());
        assertEquals("hashed_pw_123", user.getPasswordHash());
        assertEquals(Role.Admin, user.getRole());
        assertFalse(user.isEnabled());
    }

    @Test
    @DisplayName("Constructors initialize defaults and provided arguments properly")
    void testConstructors() {
        User u1 = new User(null, "alice", "hashA", Role.Employee, true);
        assertEquals("alice", u1.getUsername());
        assertEquals("hashA", u1.getPasswordHash());
        assertEquals(Role.Employee, u1.getRole());
        assertTrue(u1.isEnabled());

        User u2 = new User(null, "bob", "hashB", Role.Manager, true);
        assertEquals("bob", u2.getUsername());
        assertEquals("hashB", u2.getPasswordHash());
        assertEquals(Role.Manager, u2.getRole());
        assertTrue(u2.isEnabled());

        User u3 = new User(null, "carol", "hashC", Role.Admin, false);
        assertEquals("carol", u3.getUsername());
        assertEquals("hashC", u3.getPasswordHash());
        assertEquals(Role.Admin, u3.getRole());
        assertFalse(u3.isEnabled());

        User u4 = new User(null, "dave", "hashD", null, true);
        assertEquals(Role.Employee, u4.getRole());
    }

    @Test
    @DisplayName("Role enum contains exact required constants: Admin, Manager, Employee")
    void testRoleEnum() {
        Role[] roles = Role.values();
        assertEquals(3, roles.length);
        assertArrayEquals(new Role[]{Role.Admin, Role.Manager, Role.Employee}, roles);
        assertEquals(Role.Admin, Role.valueOf("Admin"));
        assertEquals(Role.Manager, Role.valueOf("Manager"));
        assertEquals(Role.Employee, Role.valueOf("Employee"));
    }

    @Test
    @DisplayName("Entity and table mappings match JPA specification")
    void testEntityAndTableAnnotations() {
        assertTrue(User.class.isAnnotationPresent(Entity.class));
        Table table = User.class.getAnnotation(Table.class);
        assertNotNull(table);
        assertEquals("users", table.name());
    }

    @Test
    @DisplayName("Field mappings match UUID generation, column names, constraints, and types")
    void testFieldAnnotations() throws NoSuchFieldException {
        Field idField = User.class.getDeclaredField("id");
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

        Field usernameField = User.class.getDeclaredField("username");
        assertEquals(String.class, usernameField.getType());
        Column usernameColumn = usernameField.getAnnotation(Column.class);
        assertNotNull(usernameColumn);
        assertFalse(usernameColumn.nullable());
        assertTrue(usernameColumn.unique());

        Field passwordHashField = User.class.getDeclaredField("passwordHash");
        assertEquals(String.class, passwordHashField.getType());
        Column passwordHashColumn = passwordHashField.getAnnotation(Column.class);
        assertNotNull(passwordHashColumn);
        assertEquals("password_hash", passwordHashColumn.name());
        assertFalse(passwordHashColumn.nullable());

        Field roleField = User.class.getDeclaredField("role");
        assertEquals(Role.class, roleField.getType());
        Enumerated enumerated = roleField.getAnnotation(Enumerated.class);
        assertNotNull(enumerated);
        assertEquals(EnumType.STRING, enumerated.value());
        Column roleColumn = roleField.getAnnotation(Column.class);
        assertNotNull(roleColumn);
        assertFalse(roleColumn.nullable());

        Field enabledField = User.class.getDeclaredField("enabled");
        assertEquals(boolean.class, enabledField.getType());
        Column enabledColumn = enabledField.getAnnotation(Column.class);
        assertNotNull(enabledColumn);
        assertFalse(enabledColumn.nullable());
    }

    @Test
    @DisplayName("toString must not contain password hash")
    void testToStringSecurity() {
        User user = new User(null, "admin_user", "sensitive_hash_value_999", Role.Admin, true);
        String asString = user.toString();

        assertNotNull(asString);
        assertFalse(asString.contains("sensitive_hash_value_999"), "toString must not expose passwordHash");
        assertFalse(asString.contains("password_hash"), "toString must not expose passwordHash label");
        assertTrue(asString.contains("admin_user"));
        assertTrue(asString.contains("Admin"));
        assertTrue(asString.contains("true"));
    }

    @Test
    @DisplayName("equals and hashCode do not depend on mutable fields")
    void testEqualsAndHashCode() {
        User u1 = new User(null, "user1", "hash1", Role.Employee, true);
        User u2 = new User(null, "user1", "hash1", Role.Employee, true);

        // Before persistence (id == null), equality is identity-based
        assertEquals(u1, u1);
        assertNotEquals(u1, u2);

        // Once id is set, equality is based on non-null id and independent of mutable fields
        UUID id = UUID.randomUUID();
        u1.setId(id);
        u2.setId(id);
        assertEquals(u1, u2);

        // Mutating fields does not alter equality when ids match
        u2.setUsername("user2_changed");
        u2.setPasswordHash("hash2_changed");
        u2.setRole(Role.Admin);
        u2.setEnabled(false);
        assertEquals(u1, u2);

        // Mutating fields does not alter hashCode
        int h1 = u1.hashCode();
        u1.setUsername("mutated");
        u1.setRole(Role.Manager);
        assertEquals(h1, u1.hashCode());
    }
}
