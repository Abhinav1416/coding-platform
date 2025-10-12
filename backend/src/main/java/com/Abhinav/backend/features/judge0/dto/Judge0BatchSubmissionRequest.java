package com.Abhinav.backend.features.judge0.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Judge0BatchSubmissionRequest(
        @JsonProperty("submissions") List<Judge0SubmissionRequest> submissions
) {}
