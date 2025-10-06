package com.Abhinav.backend.features.match.dto;

import com.Abhinav.backend.features.match.model.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LobbyStateDTO {
    private UUID matchId;
    private Long playerOneId;
    private String playerOneUsername; // <-- ADDED
    private Long playerTwoId;
    private String playerTwoUsername; // <-- ADDED
    private MatchStatus status;
    private Instant scheduledAt;
    private Integer durationInMinutes;
}