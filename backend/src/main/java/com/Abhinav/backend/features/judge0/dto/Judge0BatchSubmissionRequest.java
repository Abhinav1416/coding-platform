package com.Abhinav.backend.features.judge0.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the entire JSON body for a Judge0 batch submission request.
 * The top-level JSON object has a single key "submissions" which is a list.
 * Using a specific record like this is more robust than a generic Map.
 */
public record Judge0BatchSubmissionRequest(
        @JsonProperty("submissions") List<Judge0SubmissionRequest> submissions
) {}
