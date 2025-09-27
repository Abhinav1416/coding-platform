package com.Abhinav.backend.features.judge0.dto;

public record Judge0SubmissionRequest(
        String source_code,
        Integer language_id,
        String stdin,
        String expected_output
) {}
