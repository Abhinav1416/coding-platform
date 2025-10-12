package com.Abhinav.backend.features.submission.service;


import com.Abhinav.backend.features.submission.dto.SubmissionDetailsDTO;
import com.Abhinav.backend.features.submission.dto.SubmissionRequest;
import com.Abhinav.backend.features.submission.model.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SubmissionService {

    Submission createSubmission(SubmissionRequest request, Long userId);


    void processSubmission(UUID submissionId);



    Page<Submission> getSubmissionsForProblemAndUser(UUID problemId, Long userId, Pageable pageable);


    SubmissionDetailsDTO getSubmissionDetails(UUID submissionId);
}