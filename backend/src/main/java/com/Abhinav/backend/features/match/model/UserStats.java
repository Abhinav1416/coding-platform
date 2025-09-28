package com.Abhinav.backend.features.match.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_stats")
public class UserStats {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Builder.Default
    private int duelsPlayed = 0;

    @Builder.Default
    private int duelsWon = 0;

    @Builder.Default
    private int duelsLost = 0;

    @Builder.Default
    private int duelsDrawn = 0;
}