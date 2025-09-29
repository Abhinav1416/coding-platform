package com.Abhinav.backend.features.match.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class PlayerResultDTO {
    private Long userId;
    private boolean solved;
    private Instant finishTime;
    private int penalties;
    private Duration effectiveTime;
    private List<SubmissionTimelineDTO> submissions;
}
