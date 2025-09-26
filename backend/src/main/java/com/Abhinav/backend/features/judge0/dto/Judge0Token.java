package com.Abhinav.backend.features.judge0.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Judge0Token(String token) {}