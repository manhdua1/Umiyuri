package com.pandora.coreservice.config;

import com.pandora.coreservice.entity.Role;
import com.pandora.coreservice.entity.User;
import com.pandora.coreservice.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    public static final String ADMIN_USERNAME = "test_admin";
    public static final String MANAGER_USERNAME = "test_manager";
    public static final String EMPLOYEE_USERNAME = "test_employee";

    /**
     * Public demonstration password requested explicitly by user for test convenience.
     * WARNING: Not for production use.
     */
    public static final String TEST_PASSWORD = "123456";

    private static final List<SeedAccount> SEED_ACCOUNTS = List.of(
            new SeedAccount(ADMIN_USERNAME, Role.Admin),
            new SeedAccount(MANAGER_USERNAME, Role.Manager),
            new SeedAccount(EMPLOYEE_USERNAME, Role.Employee)
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (SeedAccount account : SEED_ACCOUNTS) {
            initAccountIfMissing(account.username(), account.role());
        }
    }

    private void initAccountIfMissing(String username, Role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            log.debug("Account {} already exists, skipping initialization", username);
            return;
        }

        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(encodedPassword);
        user.setRole(role);
        user.setEnabled(true);

        userRepository.save(user);
        log.info("Initialized default test account: {}", username);
    }

    private record SeedAccount(String username, Role role) {
    }
}
