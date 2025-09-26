package com.Abhinav.backend.features.judge0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record Judge0BatchTokenResponse(
        @JsonProperty("submissions") List<Judge0Token> submissions
) {}
