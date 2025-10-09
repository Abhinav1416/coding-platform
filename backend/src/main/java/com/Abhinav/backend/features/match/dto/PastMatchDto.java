package com.Abhinav.backend.features.match.dto;

import com.Abhinav.backend.features.match.model.MatchStatus;
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
public class PastMatchDto {
    private UUID matchId;
    private MatchStatus status;
    private String result;
    private Long opponentId;
    private String opponentUsername;
    private UUID problemId;
    private String problemTitle;
    private Instant endedAt;
    private Instant createdAt;
}