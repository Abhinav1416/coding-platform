package com.Abhinav.backend.features.notification.service;


import com.Abhinav.backend.features.submission.dto.SubmissionResultDTO;

import java.util.UUID;

public interface NotificationService {
    // Add the submissionId parameter
    void notifyUser(Long userId, UUID submissionId, SubmissionResultDTO result);
}