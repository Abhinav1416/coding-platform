package com.Abhinav.backend.features.match.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_code", nullable = false, unique = true)
    private String roomCode;

    @Column(name = "player_one_id", nullable = false)
    private Long playerOneId;

    @Column(name = "player_two_id")
    private Long playerTwoId;

    @Column(name = "problem_id")
    private UUID problemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    @Column(name = "difficulty_min", nullable = false)
    private Integer difficultyMin;

    @Column(name = "difficulty_max", nullable = false)
    private Integer difficultyMax;

    @Column(name = "duration_in_minutes", nullable = false)
    private Integer durationInMinutes;

    @Column(name = "start_delay_in_minutes", nullable = false)
    private Integer startDelayInMinutes;

    @Column(name = "winner_id")
    private Long winnerId;

    /**
     * Changed from Integer to primitive int.
     * This solves both the NullPointerException on match timeout and the
     * Lombok @Builder warning, as 'int' cannot be null and defaults to 0.
     */
    @Column(name = "player_one_penalties")
    private int playerOnePenalties;

    /**
     * Changed from Integer to primitive int.
     * This solves both the NullPointerException on match timeout and the
     * Lombok @Builder warning, as 'int' cannot be null and defaults to 0.
     */
    @Column(name = "player_two_penalties")
    private int playerTwoPenalties;

    @Column(name = "player_one_finish_time")
    private Instant playerOneFinishTime;

    @Column(name = "player_two_finish_time")
    private Instant playerTwoFinishTime;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;
}