package com.Abhinav.backend.features.submission.dto;


import com.Abhinav.backend.features.submission.model.Language;
import com.Abhinav.backend.features.submission.model.Submission;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SubmissionSummaryDTO {
    private UUID id;
    private String status;
    private Language language;
    private Integer runtimeMs;
    private Instant createdAt;

    public static SubmissionSummaryDTO fromEntity(Submission submission) {
        return SubmissionSummaryDTO.builder()
                .id(submission.getId())
                .status(submission.getStatus())
                .language(submission.getLanguage())
                .runtimeMs(submission.getRuntimeMs())
                .createdAt(submission.getCreatedAt())
                .build();
    }
}