package com.Abhinav.backend.features.submissions.dto;

import com.Abhinav.backend.features.submissions.model.Submission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * A detailed DTO representing the final result of a submission.
 * This is the object that will be pushed to the client via WebSockets.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResultDTO {

    private UUID submissionId;
    private String status;
    private Integer runtimeMs;
    private Integer memoryKb;
    private String stdout;
    private String stderr;
    private Instant createdAt;
    private String language;

    /**
     * A factory method to easily convert a Submission entity into this DTO.
     */
    public static SubmissionResultDTO fromEntity(Submission submission) {
        return SubmissionResultDTO.builder()
                .submissionId(submission.getId())
                .status(submission.getStatus())
                .runtimeMs(submission.getRuntimeMs())
                .memoryKb(submission.getMemoryKb())
                .stdout(submission.getStdout())
                .stderr(submission.getStderr())
                .createdAt(submission.getCreatedAt())
                .language(submission.getLanguage().getSlug())
                .build();
    }
}