package com.pandora.coreservice.config;

import com.pandora.coreservice.entity.Role;
import com.pandora.coreservice.entity.User;
import com.pandora.coreservice.repo.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder mockPasswordEncoder;

    @Test
    @DisplayName("missing all creates 3 accounts with exact roles, BCrypt encoding, and enabled true")
    void testMissingAllCreatesThreeWithExactRolesAndBCryptEncoding() {
        PasswordEncoder realEncoder = new BCryptPasswordEncoder();
        DataInitializer initializer = new DataInitializer(userRepository, realEncoder);

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        initializer.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(3)).save(captor.capture());

        List<User> savedUsers = captor.getAllValues();
        assertThat(savedUsers).hasSize(3);

        User admin = savedUsers.stream()
                .filter(u -> DataInitializer.ADMIN_USERNAME.equals(u.getUsername()))
                .findFirst()
                .orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.Admin);
        assertThat(admin.isEnabled()).isTrue();
        assertThat(admin.getId()).isNull();
        assertThat(admin.getPasswordHash()).isNotEqualTo(DataInitializer.TEST_PASSWORD);
        assertThat(realEncoder.matches(DataInitializer.TEST_PASSWORD, admin.getPasswordHash())).isTrue();

        User manager = savedUsers.stream()
                .filter(u -> DataInitializer.MANAGER_USERNAME.equals(u.getUsername()))
                .findFirst()
                .orElseThrow();
        assertThat(manager.getRole()).isEqualTo(Role.Manager);
        assertThat(manager.isEnabled()).isTrue();
        assertThat(manager.getId()).isNull();
        assertThat(manager.getPasswordHash()).isNotEqualTo(DataInitializer.TEST_PASSWORD);
        assertThat(realEncoder.matches(DataInitializer.TEST_PASSWORD, manager.getPasswordHash())).isTrue();

        User employee = savedUsers.stream()
                .filter(u -> DataInitializer.EMPLOYEE_USERNAME.equals(u.getUsername()))
                .findFirst()
                .orElseThrow();
        assertThat(employee.getRole()).isEqualTo(Role.Employee);
        assertThat(employee.isEnabled()).isTrue();
        assertThat(employee.getId()).isNull();
        assertThat(employee.getPasswordHash()).isNotEqualTo(DataInitializer.TEST_PASSWORD);
        assertThat(realEncoder.matches(DataInitializer.TEST_PASSWORD, employee.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("existing all does not save or encode and leaves accounts unchanged")
    void testExistingAllLeavesAccountsUnchanged() {
        DataInitializer initializer = new DataInitializer(userRepository, mockPasswordEncoder);

        User existingAdmin = new User(UUID.randomUUID(), DataInitializer.ADMIN_USERNAME, "hash_admin", Role.Admin, false);
        User existingManager = new User(UUID.randomUUID(), DataInitializer.MANAGER_USERNAME, "hash_manager", Role.Employee, true);
        User existingEmployee = new User(UUID.randomUUID(), DataInitializer.EMPLOYEE_USERNAME, "hash_employee", Role.Employee, false);

        when(userRepository.findByUsername(DataInitializer.ADMIN_USERNAME)).thenReturn(Optional.of(existingAdmin));
        when(userRepository.findByUsername(DataInitializer.MANAGER_USERNAME)).thenReturn(Optional.of(existingManager));
        when(userRepository.findByUsername(DataInitializer.EMPLOYEE_USERNAME)).thenReturn(Optional.of(existingEmployee));

        initializer.run(null);

        verify(userRepository, never()).save(any());
        verify(mockPasswordEncoder, never()).encode(any());

        assertThat(existingAdmin.getPasswordHash()).isEqualTo("hash_admin");
        assertThat(existingAdmin.isEnabled()).isFalse();
        assertThat(existingAdmin.getRole()).isEqualTo(Role.Admin);

        assertThat(existingManager.getPasswordHash()).isEqualTo("hash_manager");
        assertThat(existingManager.getRole()).isEqualTo(Role.Employee);
        assertThat(existingManager.isEnabled()).isTrue();

        assertThat(existingEmployee.getPasswordHash()).isEqualTo("hash_employee");
        assertThat(existingEmployee.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("mixed scenario creates and encodes only missing account")
    void testMixedOnlyMissingCreated() {
        DataInitializer initializer = new DataInitializer(userRepository, mockPasswordEncoder);

        User existingAdmin = new User(UUID.randomUUID(), DataInitializer.ADMIN_USERNAME, "hash_admin", Role.Admin, true);
        User existingEmployee = new User(UUID.randomUUID(), DataInitializer.EMPLOYEE_USERNAME, "hash_employee", Role.Employee, true);

        when(userRepository.findByUsername(DataInitializer.ADMIN_USERNAME)).thenReturn(Optional.of(existingAdmin));
        when(userRepository.findByUsername(DataInitializer.MANAGER_USERNAME)).thenReturn(Optional.empty());
        when(userRepository.findByUsername(DataInitializer.EMPLOYEE_USERNAME)).thenReturn(Optional.of(existingEmployee));

        when(mockPasswordEncoder.encode(DataInitializer.TEST_PASSWORD)).thenReturn("encoded_manager_hash");

        initializer.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(captor.capture());
        verify(mockPasswordEncoder, times(1)).encode(DataInitializer.TEST_PASSWORD);

        User savedUser = captor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo(DataInitializer.MANAGER_USERNAME);
        assertThat(savedUser.getRole()).isEqualTo(Role.Manager);
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded_manager_hash");
        assertThat(savedUser.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("running twice achieves simulated repository idempotence")
    void testRunTwiceSimulatedRepositoryIdempotence() {
        Map<String, User> simulatedDb = new HashMap<>();

        when(userRepository.findByUsername(anyString())).thenAnswer(invocation -> {
            String username = invocation.getArgument(0);
            return Optional.ofNullable(simulatedDb.get(username));
        });

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            simulatedDb.put(user.getUsername(), user);
            return user;
        });

        PasswordEncoder spyEncoder = Mockito.spy(new BCryptPasswordEncoder());
        DataInitializer initializer = new DataInitializer(userRepository, spyEncoder);

        // Run 1: seeds missing accounts
        initializer.run(null);
        assertThat(simulatedDb).hasSize(3);
        verify(userRepository, times(3)).save(any(User.class));
        verify(spyEncoder, times(3)).encode(DataInitializer.TEST_PASSWORD);

        // Run 2: all accounts exist, no new saves or encodings
        initializer.run(null);
        assertThat(simulatedDb).hasSize(3);
        verify(userRepository, times(3)).save(any(User.class));
        verify(spyEncoder, times(3)).encode(DataInitializer.TEST_PASSWORD);
    }

    @Test
    @DisplayName("ensure enabled flag is true for all created accounts")
    void testEnsureEnabled() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(mockPasswordEncoder.encode(anyString())).thenReturn("mock_hash");

        DataInitializer initializer = new DataInitializer(userRepository, mockPasswordEncoder);
        initializer.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(3)).save(captor.capture());

        assertThat(captor.getAllValues()).allMatch(User::isEnabled);
    }
}
