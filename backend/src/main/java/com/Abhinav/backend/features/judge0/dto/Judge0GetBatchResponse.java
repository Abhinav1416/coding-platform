package com.Abhinav.backend.features.judge0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents the JSON response when retrieving a batch of submission results.
 * The top-level JSON object has a single key "submissions" which is a list of results.
 */
public record Judge0GetBatchResponse(
        @JsonProperty("submissions") List<Judge0SubmissionResponse> submissions
) {}