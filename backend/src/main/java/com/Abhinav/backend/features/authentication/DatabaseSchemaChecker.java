package com.Abhinav.backend.features.authentication;

import com.Abhinav.backend.features.authentication.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DatabaseSchemaChecker implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSchemaChecker.class);

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        logger.info("--- DATABASE SCHEMA CHECKER IS RUNNING ---");
        try {
            long count = roleRepository.count();
            logger.info("✅ SUCCESS: The 'roles' table exists and is accessible. Found {} rows.", count);
        } catch (Exception e) {
            logger.error("❌ FAILURE: The 'roles' table does not exist or is not accessible.");
            logger.error("❌ This is likely the reason 'ddl-auto=update' is not working.");
            logger.error("❌ Root Cause: {}", e.getMessage());
        }
        logger.info("--- DATABASE SCHEMA CHECKER HAS FINISHED ---");
    }
}