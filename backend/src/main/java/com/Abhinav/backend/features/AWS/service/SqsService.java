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

    public void sendSubmissionMessage(UUID submissionId) {
        String logPrefix = "[SUBMISSION " + submissionId + "]";
        logger.info("{} -> Attempting to send message to SQS.", logPrefix);

        try {
            logger.debug("{} Getting queue URL for queue name: '{}'", logPrefix, submissionQueueName);
            String queueUrl = sqsAsyncClient.getQueueUrl(GetQueueUrlRequest.builder()
                    .queueName(submissionQueueName)
                    .build()).get().queueUrl();
            logger.debug("{} Successfully retrieved queue URL: {}", logPrefix, queueUrl);

            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(submissionId.toString())
                    .build();

            logger.debug("{} Dispatching message to SQS...", logPrefix);
            sqsAsyncClient.sendMessage(sendMessageRequest)
                    .whenComplete((response, throwable) -> {
                        if (throwable != null) {
                            logger.error("{} Asynchronous send to SQS queue '{}' failed.",
                                    logPrefix, submissionQueueName, throwable);
                        } else {
                            logger.info("{} Successfully sent message to SQS. SQS Message ID: {}",
                                    logPrefix, response.messageId());
                        }
                    });

            logger.info("{} <- Message dispatch initiated. Method is returning while send completes in background.", logPrefix);

        } catch (InterruptedException | ExecutionException e) {
            logger.error("{} Failed to get SQS queue URL for queue name '{}'. Cannot send message.",
                    logPrefix, submissionQueueName, e);

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Error preparing to send message to SQS for submission " + submissionId, e);
        }
    }
}
