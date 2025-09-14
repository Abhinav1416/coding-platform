package com.Abhinav.backend.features.problems.models;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "problems")
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String constraints;

    @Column(nullable = false)
    private Integer points;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "user_boilerplate_code", columnDefinition = "jsonb")
    private String userBoilerplateCode;

    @Column(name = "time_limit_ms", nullable = false)
    private Integer timeLimitMs;

    @Column(name = "memory_limit_kb", nullable = false)
    private Integer memoryLimitKb;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sample_test_cases", columnDefinition = "jsonb")
    private String sampleTestCases;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestCase> hiddenTestCases = new ArrayList<>();
}