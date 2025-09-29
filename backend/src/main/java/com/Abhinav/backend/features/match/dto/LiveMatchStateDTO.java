package com.Abhinav.backend.features.match.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveMatchStateDTO {
    private UUID matchId;
    private UUID problemId;
    private Long playerOneId;
    private Long playerTwoId;

    @Builder.Default
    private int playerOnePenalties = 0;
    @Builder.Default
    private int playerTwoPenalties = 0;

    private Instant playerOneFinishTime;
    private Instant playerTwoFinishTime;

    private Instant startedAt;
    private Integer durationInMinutes;
}