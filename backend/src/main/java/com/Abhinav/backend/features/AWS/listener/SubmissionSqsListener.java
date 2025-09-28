package com.Abhinav.backend.features.AWS.listener;


import com.Abhinav.backend.features.submission.service.SubmissionService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SubmissionSqsListener {

    private final SubmissionService submissionService;
    private static final Logger logger = LoggerFactory.getLogger(SubmissionSqsListener.class);

    /**
     * Listens to the configured SQS queue for submission messages.
     * When a message arrives, this method is invoked to process it.
     *
     * @param message The body of the SQS message, expected to be a submissionId UUID.
     */
    @SqsListener("${aws.sqs.queue-name}")
    public void receiveMessage(String message) {
        String logPrefix = "[SQS_MSG " + message + "]";
        logger.info("{} -> Received SQS message.", logPrefix);

        try {
            logger.debug("{} Attempting to parse message body into a UUID.", logPrefix);
            UUID submissionId = UUID.fromString(message);

            logPrefix = "[SUBMISSION " + submissionId + "]";
            logger.info("{} Successfully parsed UUID. Proceeding to process submission.", logPrefix);

            submissionService.processSubmission(submissionId);

            logger.info("{} <- Successfully processed submission.", logPrefix);

        } catch (IllegalArgumentException e) {
            logger.error("{} The received message is not a valid UUID string. Message will be discarded.", logPrefix, e);
        } catch (Exception e) {
            logger.error("{} An unexpected error occurred during submission processing.", logPrefix, e);

            throw new RuntimeException("Failed to process submission for message: " + message, e);
        }
    }
}