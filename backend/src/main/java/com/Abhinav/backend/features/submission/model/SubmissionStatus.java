package com.Abhinav.backend.features.submission.model;

public enum SubmissionStatus {
    PENDING,
    PROCESSING,
    ACCEPTED,
    WRONG_ANSWER,
    TIME_LIMIT_EXCEEDED,
    COMPILATION_ERROR,
    RUNTIME_ERROR,
    INTERNAL_ERROR
}