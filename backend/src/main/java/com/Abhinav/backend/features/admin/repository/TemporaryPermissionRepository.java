package com.Abhinav.backend.features.admin.repository;

import com.Abhinav.backend.features.admin.model.PermissionType;
import com.Abhinav.backend.features.admin.model.TemporaryPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemporaryPermissionRepository extends JpaRepository<TemporaryPermission, UUID> {



    @Query("SELECT p FROM TemporaryPermission p WHERE p.user.id = :userId " +
            "AND p.permissionType = 'CREATE_PROBLEM' AND p.consumed = false " +
            "AND p.expiryTimestamp > :now")
    Optional<TemporaryPermission> findActiveCreatePermissionForUser(Long userId, LocalDateTime now);


    @Query("SELECT p FROM TemporaryPermission p WHERE p.user.id = :userId " +
            "AND p.permissionType = :type AND p.targetProblemId = :problemId " +
            "AND p.consumed = false AND p.expiryTimestamp > :now")
    Optional<TemporaryPermission> findActivePermissionForProblem(Long userId, UUID problemId, PermissionType type, LocalDateTime now);


    void deleteAllByExpiryTimestampBefore(LocalDateTime expiryTimestamp);
}