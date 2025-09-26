package com.Abhinav.backend.features.AWS.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class SqsService {

    private final SqsAsyncClient sqsAsyncClient;
    private static final Logger logger = LoggerFactory.getLogger(SqsService.class);

    @Value("${aws.sqs.queue-name}")
    private String submissionQueueName;

    /**
     * Asynchronously sends a submission ID to the configured SQS queue.
     * This method handles the asynchronous result to log the actual success or failure of the send operation.
     *
     * @param submissionId The UUID of the submission to be sent.
     */
    public void sendSubmissionMessage(UUID submissionId) {
        String logPrefix = "[SUBMISSION " + submissionId + "]";
        logger.info("{} -> Attempting to send message to SQS.", logPrefix);

        try {
            // Step 1: Get the queue URL. This is a blocking call and a prerequisite.
            // If this fails, we cannot proceed.
            logger.debug("{} Getting queue URL for queue name: '{}'", logPrefix, submissionQueueName);
            String queueUrl = sqsAsyncClient.getQueueUrl(GetQueueUrlRequest.builder()
                    .queueName(submissionQueueName)
                    .build()).get().queueUrl();
            logger.debug("{} Successfully retrieved queue URL: {}", logPrefix, queueUrl);

            // Step 2: Build the request to send the message.
            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(submissionId.toString())
                    .build();

            // Step 3: Send the message asynchronously and handle the completion callback.
            logger.debug("{} Dispatching message to SQS...", logPrefix);
            sqsAsyncClient.sendMessage(sendMessageRequest)
                    .whenComplete((response, throwable) -> {
                        // This callback executes when the async operation is complete.
                        if (throwable != null) {
                            // This block executes if the async SQS send operation failed.
                            logger.error("{} Asynchronous send to SQS queue '{}' failed.",
                                    logPrefix, submissionQueueName, throwable);
                        } else {
                            // This block executes if the async SQS send operation succeeded.
                            logger.info("{} Successfully sent message to SQS. SQS Message ID: {}",
                                    logPrefix, response.messageId());
                        }
                    });

            logger.info("{} <- Message dispatch initiated. Method is returning while send completes in background.", logPrefix);

        } catch (InterruptedException | ExecutionException e) {
            // This catch block will now primarily handle failures from the blocking getQueueUrl() call.
            logger.error("{} Failed to get SQS queue URL for queue name '{}'. Cannot send message.",
                    logPrefix, submissionQueueName, e);

            if (e instanceof InterruptedException) {
                // Restore the interrupted status
                Thread.currentThread().interrupt();
            }
            // Re-throwing as a RuntimeException to signal a critical failure in the calling method.
            // This could be caught by a global exception handler to return a 500 status.
            throw new RuntimeException("Error preparing to send message to SQS for submission " + submissionId, e);
        }
    }
}
