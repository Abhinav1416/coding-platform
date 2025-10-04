package com.Abhinav.backend.features.notification.service;


import com.Abhinav.backend.features.submission.dto.SubmissionResultDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void notifyUser(Long userId, UUID submissionId, SubmissionResultDTO result) {
        String logPrefix = String.format("[WS_NOTIFY userId=%d submissionId=%s]", userId, submissionId);
        final String destination = "/topic/submission-result/" + submissionId;

        logger.info("{} -> Attempting to send notification for result '{}' to destination: {}",
                logPrefix, result.getStatus(), destination);
        try {
            messagingTemplate.convertAndSend(destination, result);
            logger.info("{} <- Successfully sent WebSocket notification.", logPrefix);
        } catch (Exception e) {
            logger.error("{} Failed to send WebSocket notification.", logPrefix, e);
        }
    }
}
