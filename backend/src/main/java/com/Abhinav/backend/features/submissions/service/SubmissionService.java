package com.Abhinav.backend.features.submissions.service;


import com.Abhinav.backend.features.submissions.dto.SubmissionDetailsDTO;
import com.Abhinav.backend.features.submissions.dto.SubmissionRequest;
import com.Abhinav.backend.features.submissions.model.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SubmissionService {

    /**
     * Handles the initial, fast part of the submission process.
     * Creates a submission record in the database with a 'PENDING' status and
     * queues it for processing.
     *
     * @param request The user's submission data (code, language, problemId).
     * @param userId  The ID of the user making the submission.
     * @return The newly created Submission entity.
     */
    Submission createSubmission(SubmissionRequest request, Long userId);

    /**
     * Handles the slow, background processing of a single submission.
     * This method will be called by our SQS listener.
     *
     * @param submissionId The ID of the submission to process.
     */
    void processSubmission(UUID submissionId);


    /**
     * Retrieves a paginated list of submissions for a specific problem and user.
     *
     * @param problemId The ID of the problem.
     * @param userId    The ID of the user.
     * @param pageable  Pagination information (page number, size, sort).
     * @return A Page of Submission entities.
     */
    Page<Submission> getSubmissionsForProblemAndUser(UUID problemId, Long userId, Pageable pageable);

    // Add this new method to your interface
    SubmissionDetailsDTO getSubmissionDetails(UUID submissionId);
}