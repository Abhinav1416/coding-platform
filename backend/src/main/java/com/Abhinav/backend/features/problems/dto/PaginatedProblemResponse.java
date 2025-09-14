package com.Abhinav.backend.features.problems.dto;


import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PaginatedProblemResponse {
    private List<ProblemSummaryResponse> problems;
    private int currentPage;
    private int totalPages;
    private long totalItems;
}