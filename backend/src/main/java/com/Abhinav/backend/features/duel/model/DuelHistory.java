package com.Abhinav.backend.features.duel.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "duel_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuelHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID duelId;

    private Long player1Id;
    private Long player2Id;

    private String player1Handle;
    private String player2Handle;

    private Integer player1Score;
    private Integer player2Score;

    private String winnerHandle;
    private Long winnerId;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    @Column(columnDefinition = "TEXT")
    private String scoreboardJson;
}