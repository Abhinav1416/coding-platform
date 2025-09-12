package com.Abhinav.backend.features.problemManagement.PMModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "problems")
@NoArgsConstructor
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String constraints;

    @Column(nullable = false)
    private Integer points;

    // Stores the JSON object as a string.
    // e.g., {"71": "class Solution { ... }", "62": "public class Solution { ... }"}
    @Column(name = "user_boilerplate_code", columnDefinition = "TEXT")
    private String userBoilerplateCode;

    @Column(name = "time_limit_ms", nullable = false)
    private Integer timeLimitMs;

    @Column(name = "memory_limit_kb", nullable = false)
    private Integer memoryLimitKb;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;


    // When a Problem is deleted, its associated TestCases are also deleted.
    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<TestCase> testCases = new HashSet<>();

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
            name = "problem_tags",
            joinColumns = @JoinColumn(name = "problem_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    // Self-referencing relationship for prerequisites
    @ManyToMany
    @JoinTable(
            name = "problem_prerequisites",
            joinColumns = @JoinColumn(name = "problem_id"), // The problem that has prerequisites
            inverseJoinColumns = @JoinColumn(name = "prerequisite_id") // The problem that is a prerequisite
    )
    private Set<Problem> prerequisites = new HashSet<>();
}
