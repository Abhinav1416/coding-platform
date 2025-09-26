package com.Abhinav.backend.features.submissions.dto;


import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PaginatedSubmissionResponse {
    private List<SubmissionSummaryDTO> submissions;
    private int currentPage;
    private int totalPages;
    private long totalItems;
}