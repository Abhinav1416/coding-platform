package com.Abhinav.backend.core.startup;

import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import com.Abhinav.backend.features.authentication.model.Role;
import com.Abhinav.backend.features.authentication.model.RoleType;
import com.Abhinav.backend.features.authentication.repository.AuthenticationUserRepository;
import com.Abhinav.backend.features.authentication.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;import org.springframework.beans.factory.annotation.Value;


@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final AuthenticationUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;


    @Value("${admin.default-email}")
    private String adminEmail;

    @Value("${admin.default-password}")
    private String adminPassword;



    @Override
    public void run(String... args) throws Exception {
        logger.info(">>> Initializing application data...");

        Role adminRole = findOrCreateRole(RoleType.ROLE_ADMIN);
        Role setterRole = findOrCreateRole(RoleType.ROLE_PROBLEM_SETTER);
        Role userRole = findOrCreateRole(RoleType.ROLE_USER);

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            AuthenticationUser adminUser = new AuthenticationUser();
            adminUser.setEmail(adminEmail);
            adminUser.setPassword(passwordEncoder.encode(adminPassword));
            adminUser.setEmailVerified(true);

            adminUser.setRoles(new HashSet<>(Set.of(adminRole, setterRole, userRole)));

            userRepository.save(adminUser);
            logger.info(">>> Root Admin user created successfully!");
        } else {
            logger.info(">>> Admin user already exists. Skipping creation.");
        }
    }


    private Role findOrCreateRole(RoleType roleType) {
        return roleRepository.findByName(roleType)
                .orElseGet(() -> {
                    logger.info(">>> Creating role: {}", roleType.name());
                    return roleRepository.save(new Role(roleType));
                });
    }
}