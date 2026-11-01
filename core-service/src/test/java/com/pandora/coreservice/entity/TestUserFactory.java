package com.pandora.coreservice.entity;

import java.util.UUID;

public final class TestUserFactory {

    private TestUserFactory() {
    }

    public static User createUser(UUID id, String username, String passwordHash, Role role, boolean enabled) {
        return new User(id, username, passwordHash, role != null ? role : Role.Employee, enabled);
    }

    public static User createDefaultUser(UUID id, String username, String passwordHash) {
        return createUser(id, username, passwordHash, Role.Employee, true);
    }

    public static User createDisabledUser(UUID id, String username, String passwordHash) {
        return createUser(id, username, passwordHash, Role.Employee, false);
    }
}
