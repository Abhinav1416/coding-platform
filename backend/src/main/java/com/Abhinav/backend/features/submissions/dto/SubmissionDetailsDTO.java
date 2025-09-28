package com.Abhinav.backend.features.submissions.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SubmissionDetailsDTO {
    private UUID id;
    private UUID problemId;
    private String problemTitle;
    private String problemSlug;
    private String status;
    private String language;
    private String code;
    private Integer runtimeMs;
    private Integer memoryKb;
    private String stdout;
    private String stderr;
    private Instant createdAt;
}