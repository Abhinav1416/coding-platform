package com.Abhinav.backend.features.submission.dto;

import com.Abhinav.backend.features.submission.model.Submission;
import com.Abhinav.backend.features.submission.model.SubmissionStatus;
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
public class SubmissionResultDTO {

    private UUID submissionId;
    private UUID matchId;
    private SubmissionStatus status;
    private Integer runtimeMs;
    private Integer memoryKb;
    private String stdout;
    private String stderr;
    private Instant createdAt;
    private String language;


    public static SubmissionResultDTO fromEntity(Submission submission) {
        return SubmissionResultDTO.builder()
                .submissionId(submission.getId())
                .matchId(submission.getMatchId())
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