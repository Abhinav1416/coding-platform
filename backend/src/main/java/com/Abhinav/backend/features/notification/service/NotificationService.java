package com.Abhinav.backend.features.notification.service;


import com.Abhinav.backend.features.submission.dto.SubmissionResultDTO;

import java.util.UUID;

public interface NotificationService {
    void notifyUser(Long userId, UUID submissionId, SubmissionResultDTO result);
}