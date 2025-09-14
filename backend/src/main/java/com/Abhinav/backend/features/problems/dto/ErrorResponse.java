package com.Abhinav.backend.features.problems.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ErrorResponse {

    private int statusCode;
    private Instant timestamp;
    private String message;
    private String path;

}