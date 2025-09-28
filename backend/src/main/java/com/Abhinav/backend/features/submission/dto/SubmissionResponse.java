package com.Abhinav.backend.features.submission.dto;


import java.util.UUID;

/**
 * A simple DTO to respond to the initial submission creation request.
 * It only contains the unique ID of the submission that was just created.
 */
public record SubmissionResponse(UUID submissionId) {
}