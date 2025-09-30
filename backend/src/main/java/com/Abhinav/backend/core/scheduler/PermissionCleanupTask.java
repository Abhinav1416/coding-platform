package com.Abhinav.backend.core.scheduler;

import com.Abhinav.backend.features.admin.repository.TemporaryPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PermissionCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(PermissionCleanupTask.class);
    private final TemporaryPermissionRepository permissionRepository;



    @Scheduled(cron = "${scheduler.cleanup.permissions.cron}")
    @Transactional
    public void cleanupExpiredPermissions() {
        logger.info("Starting expired permissions cleanup task...");
        LocalDateTime now = LocalDateTime.now();
        permissionRepository.deleteAllByExpiryTimestampBefore(now);
        logger.info("Expired permissions cleanup task finished.");
    }
}