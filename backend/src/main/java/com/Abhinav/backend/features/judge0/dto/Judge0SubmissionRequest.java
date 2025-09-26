package com.Abhinav.backend.features.judge0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Judge0SubmissionRequest(
        @JsonProperty("source_code") String sourceCode,
        @JsonProperty("language_id") Integer languageId,
        String stdin,
        @JsonProperty("expected_output") String expectedOutput
) {}
