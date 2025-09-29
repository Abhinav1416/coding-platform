package com.Abhinav.backend.features.match.dto;

import com.Abhinav.backend.features.submission.model.Submission;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SubmissionTimelineDTO {
    private UUID submissionId;
    private String status;
    private Instant submittedAt;
    private Integer runtimeMs;
    private Integer memoryKb;

    public static SubmissionTimelineDTO fromEntity(Submission submission) {
        return SubmissionTimelineDTO.builder()
                .submissionId(submission.getId())
                .status(submission.getStatus())
                .submittedAt(submission.getCreatedAt())
                .runtimeMs(submission.getRuntimeMs())
                .memoryKb(submission.getMemoryKb())
                .build();
    }
}
