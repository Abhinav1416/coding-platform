package com.Abhinav.backend.features.admin.model;


import com.Abhinav.backend.features.authentication.model.AuthenticationUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "temporary_permissions")
@Getter
@Setter
@NoArgsConstructor
public class TemporaryPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AuthenticationUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionType permissionType;

    @Column(name = "target_problem_id", nullable = true)
    private UUID targetProblemId;

    @Column(nullable = false)
    private LocalDateTime expiryTimestamp;

    @Column(nullable = false)
    private boolean consumed = false;
}