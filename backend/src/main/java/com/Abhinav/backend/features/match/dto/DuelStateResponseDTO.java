package com.Abhinav.backend.features.match.dto;

import com.Abhinav.backend.features.problem.dto.ProblemDetailResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DuelStateResponseDTO {
    private LiveMatchStateDTO liveState;
    private ProblemDetailResponse problemDetails;
}