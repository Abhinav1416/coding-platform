package com.Abhinav.backend.features.notifications.service;


import com.Abhinav.backend.features.submissions.dto.SubmissionResultDTO;

import java.util.UUID;

public interface NotificationService {
    // Add the submissionId parameter
    void notifyUser(Long userId, UUID submissionId, SubmissionResultDTO result);
}