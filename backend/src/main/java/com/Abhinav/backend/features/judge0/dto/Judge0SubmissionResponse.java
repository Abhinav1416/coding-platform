package com.Abhinav.backend.features.judge0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Judge0SubmissionResponse(
        String stdout,
        String stderr,
        @JsonProperty("compile_output") String compileOutput,
        String message,
        Double time,
        Integer memory,
        Judge0SubmissionStatus status,
        String token
) {}